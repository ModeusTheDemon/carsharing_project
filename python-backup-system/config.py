import os

class Settings:
    # Собираем URL для подключения к PostgreSQL (psycopg3 драйвер)
    # Шаблон: postgresql+psycopg://username:password@localhost:5432/dbname
    # Если пароль пустой, оставляем так: postgresql+psycopg://postgres:@localhost:5432/carsharing_db
    
    DB_USER = os.getenv("DB_USER", "postgres")
    DB_PASSWORD = os.getenv("DB_PASSWORD", "1092387456XxXYyYZzZ.,..,..,.")
    DB_HOST = os.getenv("DB_HOST", "localhost")
    DB_PORT = os.getenv("DB_PORT", "5432")
    DB_NAME = os.getenv("DB_NAME", "carsharing_db")
    
    DATABASE_URL: str = f"postgresql+psycopg://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

settings = Settings()