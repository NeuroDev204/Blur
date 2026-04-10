# Blur Social Network - Deployment Package

## 📋 Requirements
- Docker Desktop installed
- 8GB RAM minimum
- 10GB free disk space
- Internet connection

## 🚀 Quick Start

### Linux/Mac:
```bash
chmod +x start.sh
./start.sh
```

### Windows:
Double-click `start.bat`

### Using Docker Compose directly:
```bash
docker compose up -d
```

---

## 🌐 Access

- **Application**: http://localhost
- **Neo4j Browser**: http://localhost:7474
    - Username: `neo4j`
    - Password: `12345678`

---

## 📊 Management

### View logs:
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f identity-service
```

### Restart services:
```bash
# All
docker compose restart

# Specific
docker compose restart identity-service
```

### Stop services:
```bash
# Linux/Mac
./stop.sh

# Windows
stop.bat

# Or
docker compose down
```

### Update to latest version:
```bash
docker compose pull
docker compose up -d
```

### Complete cleanup (removes all data):
```bash
docker compose down -v
```

---

## 🐛 Troubleshooting

### Services not starting?
```bash
# Check logs
docker compose logs

# Check status
docker compose ps
```

### Port already in use?
```bash
# Stop all containers
docker compose down

# Check what's using port
# Linux/Mac: lsof -i :80
# Windows: netstat -ano | findstr :80
```

### Reset everything:
```bash
docker compose down -v
docker system prune -a
./start.sh
```

---

## 📦 What's included?

- API Gateway (Port 8888)
- Identity Service (Port 8080)
- Profile Service (Port 8081)
- Notification Service (Port 8082)
- Chat Service (Port 8083, 8099)
- Post Service (Port 8084)
- Story Service (Port 8086)
- Frontend (Port 80)
- Neo4j (Port 7474, 7687)
- MySQL (Port 3306)
- MongoDB (Port 27017)
- Redis (Port 6379)
- Kafka (Port 9092)

---

## ⚙️ Configuration

Edit `docker-compose.yaml` to customize:
- Passwords
- Ports
- Resource limits
- Environment variables

---

## 🆘 Support

For issues, check logs:
```bash
docker compose logs -f [service-name]
```

---

## 📄 License

Copyright © 2024 Blur Social Network