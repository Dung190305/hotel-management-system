package com.qlks.server;

import com.qlks.dao.DichVuDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.dao.impl.DichVuDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.model.DichVu;
import com.qlks.model.DonDichVuPhong;
import com.qlks.model.DonDichVuPhong.ChiTietMon;
import com.qlks.model.DonDichVuPhong.HinhThucThanhToanOrder;
import com.qlks.model.Phong;
import com.qlks.service.RoomOrderService;
import com.qlks.util.FormatUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Máy chủ HTTP siêu nhẹ (Embedded HTTP Server) phục vụ việc đặt dịch vụ tại phòng qua mã QR.
 * Sử dụng thư viện chuẩn của Java 21, chạy độc lập đa luồng không gây ảnh hưởng tới giao diện Swing.
 */
public final class RoomOrderHttpServer {

    private static final Logger LOGGER = Logger.getLogger(RoomOrderHttpServer.class.getName());
    public static final int DEFAULT_PORT = 8080;

    private static volatile HttpServer server;
    private static volatile int activePort = DEFAULT_PORT;
    private static RoomOrderService orderService = new RoomOrderService();
    private static DichVuDAO dichVuDAO = new DichVuDAOImpl();
    private static PhongDAO phongDAO = new PhongDAOImpl();

    public static void setDependenciesForTesting(RoomOrderService os, DichVuDAO dv, PhongDAO p) {
        if (os != null) orderService = os;
        if (dv != null) dichVuDAO = dv;
        if (p != null) phongDAO = p;
    }

    private RoomOrderHttpServer() {
    }

    /**
     * Khởi động máy chủ HTTP phục vụ QR Room Order.
     */
    public static synchronized boolean startServer(int preferredPort) {
        if (server != null) {
            return true;
        }

        int port = preferredPort;
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                activePort = port;
                break;
            } catch (IOException e) {
                LOGGER.warning("Cổng " + port + " đang bận, thử cổng tiếp theo: " + (port + 1));
                port++;
            }
        }

        if (server == null) {
            LOGGER.severe("Không thể khởi động RoomOrderHttpServer trên các cổng từ " + preferredPort);
            return false;
        }

        // Đăng ký các context routing
        server.createContext("/order", new OrderPageHandler());
        server.createContext("/api/menu", new MenuApiHandler());
        server.createContext("/api/order", new SubmitOrderApiHandler());
        server.createContext("/api/order-status", new OrderStatusApiHandler());

        // Sử dụng Virtual Threads của Java 21 cho hiệu năng cao
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        LOGGER.info("RoomOrderHttpServer đã khởi động thành công trên cổng: " + activePort);
        return true;
    }

    public static synchronized void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
            LOGGER.info("Đã dừng RoomOrderHttpServer.");
        }
    }

    public static int getActivePort() {
        return activePort;
    }

    public static String getOrderUrl(String soPhong) {
        return String.format("http://localhost:%d/order?room=%s", activePort, soPhong != null ? soPhong.trim() : "");
    }

    /**
     * Handler trả về trang Web Menu đặt món tiếng Việt responsive trên điện thoại di động.
     */
    static class OrderPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String roomParam = getQueryParam(query, "room");
            if (roomParam == null || roomParam.isBlank()) {
                roomParam = "Phòng chưa xác định";
            }

            String html = generateOrderHtml(roomParam);
            sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
        }
    }

    /**
     * Handler trả về danh mục dịch vụ đang kinh doanh dạng JSON.
     */
    static class MenuApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<DichVu> dsDichVu = dichVuDAO.findDangKinhDoanh();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < dsDichVu.size(); i++) {
                DichVu dv = dsDichVu.get(i);
                json.append(String.format("{\"ma\":\"%s\",\"ten\":\"%s\",\"gia\":%s,\"dvt\":\"Phần\"}",
                        dv.getMaDichVu(),
                        escapeJson(dv.getTenDichVu()),
                        dv.getDonGiaHienTai().toPlainString()));
                if (i < dsDichVu.size() - 1) json.append(",");
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString(), "application/json; charset=UTF-8");
        }
    }

    /**
     * Handler tiếp nhận đơn đặt món gửi từ điện thoại khách.
     */
    static class SubmitOrderApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}", "application/json");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }

                String payload = body.toString();
                // Phân tích đơn giản các trường từ JSON payload
                String room = extractJsonField(payload, "room");
                String clientToken = extractJsonField(payload, "clientToken");
                String ghiChu = extractJsonField(payload, "ghiChu");
                String paymentMethodStr = extractJsonField(payload, "paymentMethod");
                HinhThucThanhToanOrder hinhThuc = "CHUYEN_KHOAN_VIETQR".equalsIgnoreCase(paymentMethodStr)
                        ? HinhThucThanhToanOrder.CHUYEN_KHOAN_VIETQR
                        : HinhThucThanhToanOrder.GHI_VAO_HOA_DON;

                List<ChiTietMon> items = parseItemsFromJson(payload);

                DonDichVuPhong order = orderService.taoYeuCauOrder(room, clientToken, items, ghiChu, hinhThuc);

                String resJson = String.format("{\"success\":true,\"orderId\":\"%s\",\"message\":\"Đặt dịch vụ thành công! Lễ tân đã tiếp nhận yêu cầu.\"}",
                        order.getOrderId());
                sendResponse(exchange, 200, resJson, "application/json; charset=UTF-8");

            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Lỗi tiếp nhận đơn phòng: " + ex.getMessage());
                String errJson = String.format("{\"success\":false,\"message\":\"%s\"}", escapeJson(ex.getMessage()));
                sendResponse(exchange, 400, errJson, "application/json; charset=UTF-8");
            }
        }
    }

    /**
     * Handler kiểm tra trạng thái đơn hàng thời gian thực.
     */
    static class OrderStatusApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String orderId = getQueryParam(query, "orderId");
            if (orderId == null || orderId.isBlank()) {
                sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Thiếu orderId\"}", "application/json");
                return;
            }

            Optional<DonDichVuPhong> opt = orderService.timTheoId(orderId);
            if (opt.isEmpty()) {
                sendResponse(exchange, 404, "{\"success\":false,\"message\":\"Không tìm thấy đơn hàng\"}", "application/json");
                return;
            }

            DonDichVuPhong o = opt.get();
            String json = String.format("{\"success\":true,\"orderId\":\"%s\",\"status\":\"%s\",\"statusText\":\"%s\",\"lyDo\":\"%s\"}",
                    o.getOrderId(),
                    o.getTrangThai().name(),
                    escapeJson(o.getTrangThai().getTenHienThi()),
                    escapeJson(o.getLyDoTuChoi() != null ? o.getLyDoTuChoi() : ""));
            sendResponse(exchange, 200, json, "application/json; charset=UTF-8");
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String getQueryParam(String query, String name) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length >= 2 && parts[0].equalsIgnoreCase(name)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static List<ChiTietMon> parseItemsFromJson(String json) {
        List<ChiTietMon> result = new ArrayList<>();
        // Tìm mảng items trong JSON
        int start = json.indexOf("\"items\"");
        if (start < 0) return result;
        int arrStart = json.indexOf("[", start);
        int arrEnd = json.indexOf("]", arrStart);
        if (arrStart < 0 || arrEnd < 0) return result;

        String itemsContent = json.substring(arrStart + 1, arrEnd);
        for (String block : itemsContent.split("\\}")) {
            if (!block.contains("{")) continue;
            String clean = block.substring(block.indexOf("{"));
            String ma = extractJsonField(clean, "ma");
            String ten = extractJsonField(clean, "ten");
            String soLuongStr = "";
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"soLuong\"\\s*:\\s*([0-9]+)").matcher(clean);
            if (m.find()) soLuongStr = m.group(1);

            int soLuong = 1;
            try {
                if (!soLuongStr.isBlank()) soLuong = Integer.parseInt(soLuongStr);
            } catch (Exception ignored) {}

            if (!ma.isBlank() && soLuong > 0) {
                result.add(new ChiTietMon(ma, ten, soLuong, null));
            }
        }
        return result;
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    private static String generateOrderHtml(String roomNumber) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"vi\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "    <title>Dịch vụ phòng " + roomNumber + " - Grand Luxury Hotel</title>\n" +
                "    <style>\n" +
                "        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n" +
                "        body { background-color: #f4f6f9; color: #333; padding-bottom: 90px; }\n" +
                "        .header { background: linear-gradient(135deg, #1e3c72, #2a5298); color: white; padding: 20px 16px; text-align: center; border-bottom-left-radius: 20px; border-bottom-right-radius: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }\n" +
                "        .header h1 { font-size: 20px; font-weight: 700; margin-bottom: 4px; }\n" +
                "        .badge-room { display: inline-block; background: #ffc107; color: #212529; font-weight: 700; padding: 4px 14px; border-radius: 20px; font-size: 14px; margin-top: 6px; }\n" +
                "        .container { padding: 16px; max-width: 500px; margin: 0 auto; }\n" +
                "        .menu-title { font-size: 16px; font-weight: 700; margin: 16px 0 12px 4px; color: #495057; display: flex; align-items: center; justify-content: space-between; }\n" +
                "        .menu-card { background: white; border-radius: 14px; padding: 14px; margin-bottom: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.05); display: flex; align-items: center; justify-content: space-between; }\n" +
                "        .item-info h3 { font-size: 15px; font-weight: 600; color: #212529; }\n" +
                "        .item-info .price { font-size: 14px; color: #e63946; font-weight: 700; margin-top: 4px; }\n" +
                "        .qty-controls { display: flex; align-items: center; gap: 8px; }\n" +
                "        .btn-qty { width: 32px; height: 32px; border-radius: 50%; border: 1px solid #ced4da; background: #fff; font-size: 18px; font-weight: 700; color: #2a5298; display: flex; align-items: center; justify-content: center; cursor: pointer; }\n" +
                "        .btn-qty:active { background: #e9ecef; }\n" +
                "        .qty-val { font-size: 15px; font-weight: 700; min-width: 20px; text-align: center; }\n" +
                "        .section-note { background: white; border-radius: 14px; padding: 14px; margin-top: 16px; box-shadow: 0 2px 6px rgba(0,0,0,0.05); }\n" +
                "        .section-note textarea { width: 100%; border: 1px solid #ced4da; border-radius: 8px; padding: 10px; font-size: 14px; resize: none; outline: none; margin-top: 8px; }\n" +
                "        .footer-bar { position: fixed; bottom: 0; left: 0; right: 0; background: white; padding: 12px 16px; box-shadow: 0 -4px 12px rgba(0,0,0,0.1); display: flex; align-items: center; justify-content: space-between; max-width: 500px; margin: 0 auto; }\n" +
                "        .total-box .total-label { font-size: 12px; color: #6c757d; }\n" +
                "        .total-box .total-val { font-size: 18px; font-weight: 800; color: #e63946; }\n" +
                "        .btn-submit { background: linear-gradient(135deg, #28a745, #218838); color: white; border: none; padding: 12px 24px; border-radius: 10px; font-size: 15px; font-weight: 700; cursor: pointer; box-shadow: 0 4px 10px rgba(40,167,69,0.3); }\n" +
                "        .btn-submit:disabled { background: #adb5bd; box-shadow: none; }\n" +
                "        .modal { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.6); align-items: center; justify-content: center; z-index: 1000; padding: 20px; }\n" +
                "        .modal-card { background: white; border-radius: 16px; padding: 24px; text-align: center; max-width: 380px; width: 100%; }\n" +
                "        .modal-icon { font-size: 48px; margin-bottom: 12px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"header\">\n" +
                "        <h1>GRAND LUXURY HOTEL</h1>\n" +
                "        <p style=\"font-size: 13px; opacity: 0.9;\">Dịch vụ phục vụ tận phòng (Room Service)</p>\n" +
                "        <div class=\"badge-room\">Phòng: " + roomNumber + "</div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"menu-title\"><span>Menu đồ uống & đồ ăn nhanh</span><span id=\"itemCount\" style=\"font-size: 12px; color: #6c757d;\">0 món đã chọn</span></div>\n" +
                "        <div id=\"menuList\">Đang tải menu...</div>\n" +
                "\n" +
                "        <div class=\"section-note\">\n" +
                "            <label style=\"font-size: 14px; font-weight: 600;\">Yêu cầu đặc biệt (tùy chọn):</label>\n" +
                "            <textarea id=\"note\" rows=\"2\" placeholder=\"VD: Cho thêm xô đá, nước không lạnh, xin thêm ly...\"></textarea>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"footer-bar\">\n" +
                "        <div class=\"total-box\">\n" +
                "            <div class=\"total-label\">Tổng thanh toán:</div>\n" +
                "            <div class=\"total-val\" id=\"totalPrice\">0 đ</div>\n" +
                "        </div>\n" +
                "        <button class=\"btn-submit\" id=\"btnOrder\" onclick=\"submitOrder()\" disabled>Gọi lên phòng</button>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"modal\" id=\"orderModal\">\n" +
                "        <div class=\"modal-card\">\n" +
                "            <div class=\"modal-icon\" id=\"modalIcon\">🔔</div>\n" +
                "            <h3 id=\"modalTitle\" style=\"font-size: 18px; margin-bottom: 8px;\">Đang gửi yêu cầu...</h3>\n" +
                "            <p id=\"modalMsg\" style=\"font-size: 14px; color: #6c757d; margin-bottom: 20px;\">Vui lòng chờ giây lát</p>\n" +
                "            <button style=\"background: #2a5298; color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 600;\" onclick=\"closeModal()\">Đóng</button>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        const roomNumber = '" + roomNumber + "';\n" +
                "        const clientToken = 'TKN-' + Math.random().toString(36).substring(2) + Date.now();\n" +
                "        let menu = [];\n" +
                "        let cart = {};\n" +
                "\n" +
                "        fetch('/api/menu')\n" +
                "            .then(res => res.json())\n" +
                "            .then(data => {\n" +
                "                menu = data;\n" +
                "                renderMenu();\n" +
                "            })\n" +
                "            .catch(err => {\n" +
                "                document.getElementById('menuList').innerHTML = '<p style=\"color: red;\">Không thể tải menu. Vui lòng thử lại sau.</p>';\n" +
                "            });\n" +
                "\n" +
                "        function renderMenu() {\n" +
                "            const list = document.getElementById('menuList');\n" +
                "            if (menu.length === 0) {\n" +
                "                list.innerHTML = '<p>Hiện không có dịch vụ nào đang phục vụ.</p>';\n" +
                "                return;\n" +
                "            }\n" +
                "            let html = '';\n" +
                "            menu.forEach(item => {\n" +
                "                const qty = cart[item.ma] || 0;\n" +
                "                html += `\n" +
                "                    <div class=\"menu-card\">\n" +
                "                        <div class=\"item-info\">\n" +
                "                            <h3>${item.ten}</h3>\n" +
                "                            <div class=\"price\">${Number(item.gia).toLocaleString('vi-VN')} đ / ${item.dvt}</div>\n" +
                "                        </div>\n" +
                "                        <div class=\"qty-controls\">\n" +
                "                            <button class=\"btn-qty\" onclick=\"updateQty('${item.ma}', -1)\">-</button>\n" +
                "                            <span class=\"qty-val\" id=\"qty_${item.ma}\">${qty}</span>\n" +
                "                            <button class=\"btn-qty\" onclick=\"updateQty('${item.ma}', 1)\">+</button>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                `;\n" +
                "            });\n" +
                "            list.innerHTML = html;\n" +
                "        }\n" +
                "\n" +
                "        function updateQty(id, delta) {\n" +
                "            const current = cart[id] || 0;\n" +
                "            const next = Math.max(0, current + delta);\n" +
                "            if (next === 0) delete cart[id];\n" +
                "            else cart[id] = next;\n" +
                "            const el = document.getElementById('qty_' + id);\n" +
                "            if (el) el.innerText = next;\n" +
                "            updateFooter();\n" +
                "        }\n" +
                "\n" +
                "        function updateFooter() {\n" +
                "            let total = 0;\n" +
                "            let count = 0;\n" +
                "            for (const [id, qty] of Object.entries(cart)) {\n" +
                "                const item = menu.find(m => m.ma === id);\n" +
                "                if (item) {\n" +
                "                    total += item.gia * qty;\n" +
                "                    count += qty;\n" +
                "                }\n" +
                "            }\n" +
                "            document.getElementById('totalPrice').innerText = total.toLocaleString('vi-VN') + ' đ';\n" +
                "            document.getElementById('itemCount').innerText = count + ' món đã chọn';\n" +
                "            document.getElementById('btnOrder').disabled = count === 0;\n" +
                "        }\n" +
                "\n" +
                "        function submitOrder() {\n" +
                "            const items = [];\n" +
                "            for (const [id, qty] of Object.entries(cart)) {\n" +
                "                const item = menu.find(m => m.ma === id);\n" +
                "                if (item && qty > 0) {\n" +
                "                    items.push({ ma: item.ma, ten: item.ten, soLuong: qty });\n" +
                "                }\n" +
                "            }\n" +
                "            if (items.length === 0) return;\n" +
                "\n" +
                "            const note = document.getElementById('note').value;\n" +
                "            showModal('⏳', 'Đang gửi yêu cầu...', 'Vui lòng chờ Lễ tân tiếp nhận...');\n" +
                "\n" +
                "            fetch('/api/order', {\n" +
                "                method: 'POST',\n" +
                "                headers: { 'Content-Type': 'application/json' },\n" +
                "                body: JSON.stringify({\n" +
                "                    room: roomNumber,\n" +
                "                    clientToken: clientToken,\n" +
                "                    items: items,\n" +
                "                    ghiChu: note,\n" +
                "                    paymentMethod: 'GHI_VAO_HOA_DON'\n" +
                "                })\n" +
                "            })\n" +
                "            .then(res => res.json())\n" +
                "            .then(res => {\n" +
                "                if (res.success) {\n" +
                "                    showModal('✅', 'Đặt thành công!', 'Lễ tân đã tiếp nhận yêu cầu. Nhân viên sẽ mang đồ uống lên phòng ' + roomNumber + ' trong ít phút!');\n" +
                "                    cart = {};\n" +
                "                    renderMenu();\n" +
                "                    updateFooter();\n" +
                "                } else {\n" +
                "                    showModal('⚠️', 'Không thể đặt món', res.message || 'Lỗi xử lý');\n" +
                "                }\n" +
                "            })\n" +
                "            .catch(err => {\n" +
                "                showModal('❌', 'Lỗi kết nối', 'Không thể kết nối đến máy chủ khách sạn. Vui lòng gọi lễ tân qua phím 0.');\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function showModal(icon, title, msg) {\n" +
                "            document.getElementById('modalIcon').innerText = icon;\n" +
                "            document.getElementById('modalTitle').innerText = title;\n" +
                "            document.getElementById('modalMsg').innerText = msg;\n" +
                "            document.getElementById('orderModal').style.display = 'flex';\n" +
                "        }\n" +
                "        function closeModal() {\n" +
                "            document.getElementById('orderModal').style.display = 'none';\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
