import logging
# import os
import time
import traceback
from sqlalchemy import text

# Импортируем ваш менеджер сессий и аварийный восстановитель
from database.session import get_db
from backuper import run_backuper

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - [%(filename)s] - %(message)s"
)
logger = logging.getLogger(__name__)


def is_database_alive() -> bool:
    """Проверяет, отвечает ли база данных на SQL-запросы."""
    try:
        with get_db() as db:
            db.execute(text("SELECT 1"))
            return True
    except Exception as e:
        logger.warning(f"Database ping failed! Error: {e}")
        return False


def run_checker():
    """Джоба-наблюдатель (watchdog)"""
    logger.info("=== RUNNING DATABASE HEALTH CHECK ===")

    # Проверяем здоровье базы данных
    if is_database_alive():
        logger.info("Database status: HEALTHY. No action required.")
        return

    # Если база не ответила, даем ей второй и третий шансы через 5 секунд (защита от ложных срабатываний)
    for i in range(2):
        logger.warning("Database seems down. Re-trying in 5 seconds for confirmation...")
        time.sleep(5)
        
        if is_database_alive():
            logger.info("Database recovered after retry. Status: HEALTHY.")
            return

    # 3. База гарантированно мертва. Начинаем процесс самовосстановления (Self-Healing)
    logger.critical("DATABASE IS DOWN! Starting emergency disaster recovery procedure...")

    try:
        import docker
        # Подключаемся к Docker-демону хоста
        client = docker.from_env()
        
        # Имя контейнера базы данных из вашего docker-compose.yml
        POSTGRES_CONTAINER_NAME = "carsharing-postgres"

        # Принудительно останавливаем контейнер СУБД, чтобы освободить файлы тома
        try:
            logger.info(f"Stopping container '{POSTGRES_CONTAINER_NAME}' to prevent data corruption...")
            container = client.containers.get(POSTGRES_CONTAINER_NAME)
            container.stop(timeout=10)
            logger.info("Container stopped successfully.")
        except docker.errors.NotFound:
            logger.warning(f"Container '{POSTGRES_CONTAINER_NAME}' not found. It might be already deleted or renamed.")
        except Exception as docker_err:
            logger.error(f"Could not stop container: {docker_err}. Proceeding with file restoration anyway...")

        # Вызываем нашу аварийнную джобу восстановления файлов
        logger.info("Invoking emergency file restoration job...")
        run_backuper()

        # Запускаем контейнер базы данных обратно
        logger.info(f"Starting container '{POSTGRES_CONTAINER_NAME}' back up...")
        try:
            container = client.containers.get(POSTGRES_CONTAINER_NAME)
            container.start()
            logger.info("=======================================================")
            logger.critical("SELF-HEALING COMPLETED! Database container is starting up in RECOVERY mode.")
            logger.info("=======================================================")
        except Exception as start_err:
            logger.critical(f"Failed to automatically start container back! You must start it manually. Error: {start_err}")

    except Exception as master_ex:
        logger.critical(f"Self-healing orchestrator failed: {master_ex}")
        logger.error(f"Details:\n{traceback.format_exc()}")
        raise


if __name__ == "__main__":
    run_checker()
