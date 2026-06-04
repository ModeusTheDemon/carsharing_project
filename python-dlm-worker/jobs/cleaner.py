import logging
from database.session import get_db
from database.models import MainTelemetry, ArchiveRide
from policies.rules_engine import RetentionRulesEngine

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - [%(filename)s] - %(message)s")
logger = logging.getLogger(__name__)


def run_cleaner():
    logger.info("Starting Cleaner Job: Removing old telemetry data and cleaning archive...")

    rules_engine = RetentionRulesEngine()

    try:
        telemetry_threshold = rules_engine.get_telemetry_cleanup_threshold()
        logger.info(f"Telemetry cleanup threshold: {telemetry_threshold.strftime('%Y-%m-%d %H:%M:%S %Z')}")

        with get_db() as db:
            telemetry_deleted = db.query(MainTelemetry).filter(
                MainTelemetry.timestamp < telemetry_threshold
            ).delete(synchronize_session=False)

            if telemetry_deleted > 0:
                logger.info(f"Cleaned {telemetry_deleted} telemetry records older than 15 days")
            else:
                logger.info("No old telemetry data found for cleanup")

            payment_threshold = rules_engine.get_payment_preservation_threshold()
            logger.info(f"Payment preservation threshold: {payment_threshold.strftime('%Y-%m-%d %H:%M:%S %Z')}")

            archive_deleted = db.query(ArchiveRide).filter(
                ArchiveRide.started_at < payment_threshold
            ).delete(synchronize_session=False)

            if archive_deleted > 0:
                logger.info(f"Cleaned {archive_deleted} archive ride records")
            else:
                logger.info("No old archive records found for cleanup")

            logger.info(f"Cleaner job completed: {telemetry_deleted} telemetry records, {archive_deleted} archive rides")

    except Exception as e:
        error_config = rules_engine.get_error_handling_config()
        logger.error(f"Cleaner job failed: {e}. Retry configuration: {error_config}")

        import traceback
        error_details = traceback.format_exc()
        logger.error(f"Error details: {error_details}")

        try:
            db.rollback()
            logger.info("Transaction rolled back due to error")
        except:
            logger.warning("Could not rollback transaction")

        raise


if __name__ == "__main__":
    run_cleaner()
