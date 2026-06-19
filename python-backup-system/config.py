import os

class Settings:
    # Собираем URL для подключения к PostgreSQL (psycopg3 драйвер)
    # Шаблон: postgresql+psycopg://username:password@localhost:5432/dbname
    # Если пароль пустой, оставляем так: postgresql+psycopg://postgres:@localhost:5432/carsharing_db
    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        "postgresql+psycopg://postgres:1092387456XxXYyYZzZ.,..,..,.@localhost:5432/carsharing_db",

    )

settings = Settings()