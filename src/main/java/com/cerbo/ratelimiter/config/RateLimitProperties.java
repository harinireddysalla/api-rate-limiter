package com.cerbo.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private Map<String, ClientLimit> clients = new HashMap<>();

    private Cache cache = new Cache();

    public Map<String, ClientLimit> getClients() {
        return clients;
    }

    public void setClients(Map<String, ClientLimit> clients) {
        this.clients = clients;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public static class ClientLimit {

        private Limit read = new Limit();
        private Limit write = new Limit();

        public Limit getRead() {
            return read;
        }

        public void setRead(Limit read) {
            this.read = read;
        }

        public Limit getWrite() {
            return write;
        }

        public void setWrite(Limit write) {
            this.write = write;
        }
    }

    public static class Limit {

        private long capacity;
        private long refillPerMinute;

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillPerMinute() {
            return refillPerMinute;
        }

        public void setRefillPerMinute(long refillPerMinute) {
            this.refillPerMinute = refillPerMinute;
        }
    }

    public static class Cache {

        private int maxClients = 10_000;

        public int getMaxClients() {
            return maxClients;
        }

        public void setMaxClients(int maxClients) {
            this.maxClients = maxClients;
        }
    }
}