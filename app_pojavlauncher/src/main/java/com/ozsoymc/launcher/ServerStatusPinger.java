package com.ozsoymc.launcher;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Gercek Minecraft Server List Ping. Basarisizsa null doner. Arka plan thread'inde cagir. */
public class ServerStatusPinger {
    public static class Status {
        public final int online, max;
        Status(int o, int m) { online = o; max = m; }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws Exception {
        while (true) {
            if ((value & ~0x7F) == 0) { out.writeByte(value); return; }
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    private static int readVarInt(DataInputStream in) throws Exception {
        int result = 0, shift = 0;
        while (true) {
            int b = in.readUnsignedByte();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
            if (shift > 35) throw new RuntimeException("VarInt cok uzun");
        }
    }

    public static Status ping() { return ping("mc.ozsoymc.com.tr", 25565, 4000); }

    public static Status ping(String host, int port, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.setSoTimeout(timeoutMs);
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());

            ByteArrayOutputStream hsBytes = new ByteArrayOutputStream();
            DataOutputStream hs = new DataOutputStream(hsBytes);
            writeVarInt(hs, 0x00);
            writeVarInt(hs, 760);
            byte[] h = host.getBytes("UTF-8");
            writeVarInt(hs, h.length);
            hs.write(h);
            hs.writeShort(port);
            writeVarInt(hs, 1);
            writeVarInt(out, hsBytes.size());
            out.write(hsBytes.toByteArray());
            writeVarInt(out, 1);
            writeVarInt(out, 0x00);
            out.flush();

            readVarInt(in);
            readVarInt(in);
            int len = readVarInt(in);
            byte[] buf = new byte[len];
            in.readFully(buf);
            JSONObject players = new JSONObject(new String(buf, "UTF-8")).getJSONObject("players");
            return new Status(players.getInt("online"), players.getInt("max"));
        } catch (Exception e) {
            return null;
        }
    }
}
