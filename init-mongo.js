db = db.getSiblingDB('shoot'); // 데이터베이스 선택
db.createUser({
    user: "admin",
    pwd: "admin123",
    roles: [
        { role: "readWrite", db: "shoot" }
    ]
});
