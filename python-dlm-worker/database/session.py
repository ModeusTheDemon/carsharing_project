from contextlib import contextmanager
from sqlalchemy.orm import sessionmaker
from database.models import engine # Импортируем созданный движок из соседнего файла моделей

# Создаем фабрику сессий.
# autocommit=False и autoflush=False — стандарт для безопасного контроля транзакций вручную
# через session.commit()
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

@contextmanager
def get_db():
    """
    Контекстный менеджер для работы с сессией БД.
    Использование:
        with get_db() as session:
            session.query(...)
    """
    db = SessionLocal()
    try:
        yield db
        db.commit() # Если всё прошло успешно, фиксируем изменения в БД
    except Exception as e:
        db.rollback()
        raise e
    finally:
        db.close()