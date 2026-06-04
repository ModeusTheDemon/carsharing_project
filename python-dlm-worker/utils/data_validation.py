import logging
from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime
from decimal import Decimal
from sqlalchemy.orm import Session

from database.models import MainRide, ArchiveRide, MainPayment, ArchivePayment, MainUser

logger = logging.getLogger(__name__)


class DataValidationError(Exception):
    pass


class DataIntegrityValidator:
    @staticmethod
    def validate_ride_integrity(main_ride: MainRide, archive_ride: ArchiveRide) -> List[str]:
        issues = []

        if str(main_ride.id) != str(archive_ride.id):
            issues.append(f"Ride ID mismatch: main={main_ride.id}, archive={archive_ride.id}")

        if str(main_ride.user_id) != str(archive_ride.user_id):
            issues.append(f"User ID mismatch: main={main_ride.user_id}, archive={archive_ride.user_id}")

        if str(main_ride.vehicle_id) != str(archive_ride.vehicle_id):
            issues.append(f"Vehicle ID mismatch: main={main_ride.vehicle_id}, archive={archive_ride.vehicle_id}")

        if main_ride.status != archive_ride.status:
            issues.append(f"Status mismatch: main={main_ride.status}, archive={archive_ride.status}")

        if main_ride.start_time != archive_ride.start_time:
            issues.append(f"Start time mismatch: main={main_ride.start_time}, archive={archive_ride.start_time}")

        if main_ride.end_time is not None and archive_ride.end_time is not None:
            if main_ride.end_time != archive_ride.end_time:
                issues.append(f"End time mismatch: main={main_ride.end_time}, archive={archive_ride.end_time}")

        if main_ride.total_cost is not None and archive_ride.total_cost is not None:
            if float(main_ride.total_cost) != float(archive_ride.total_cost):
                issues.append(f"Total cost mismatch: main={main_ride.total_cost}, archive={archive_ride.total_cost}")

        return issues

    @staticmethod
    def validate_payment_integrity(main_payment: MainPayment, archive_payment: ArchivePayment) -> List[str]:
        issues = []

        if str(main_payment.id) != str(archive_payment.id):
            issues.append(f"Payment ID mismatch: main={main_payment.id}, archive={archive_payment.id}")

        if str(main_payment.ride_id) != str(archive_payment.ride_id):
            issues.append(f"Ride ID mismatch: main={main_payment.ride_id}, archive={archive_payment.ride_id}")

        if str(main_payment.user_id) != str(archive_payment.user_id):
            issues.append(f"User ID mismatch: main={main_payment.user_id}, archive={archive_payment.user_id}")

        if float(main_payment.amount) != float(archive_payment.amount):
            issues.append(f"Amount mismatch: main={main_payment.amount}, archive={archive_payment.amount}")

        if main_payment.currency != archive_payment.currency:
            issues.append(f"Currency mismatch: main={main_payment.currency}, archive={archive_payment.currency}")

        if main_payment.status != archive_payment.status:
            issues.append(f"Status mismatch: main={main_payment.status}, archive={archive_payment.status}")

        if main_payment.payment_method is not None and archive_payment.payment_method is not None:
            if main_payment.payment_method != archive_payment.payment_method:
                issues.append(f"Payment method mismatch: main={main_payment.payment_method}, archive={archive_payment.payment_method}")

        if main_payment.created_at != archive_payment.created_at:
            issues.append(f"Creation time mismatch: main={main_payment.created_at}, archive={archive_payment.created_at}")

        return issues

    @staticmethod
    def validate_user_references(db: Session, user_id: str) -> List[str]:
        issues = []

        user = db.query(MainUser).filter(MainUser.id == user_id).first()
        if not user:
            issues.append(f"User with ID {user_id} does not exist")

        return issues

    @staticmethod
    def validate_batch_consistency(main_records: List[Any], archive_records: List[Any]) -> List[str]:
        issues = []

        if len(main_records) != len(archive_records):
            issues.append(f"Record count mismatch: main={len(main_records)}, archive={len(archive_records)}")

        main_ids = {str(record.id) for record in main_records}
        archive_ids = {str(record.id) for record in archive_records}

        missing_in_archive = main_ids - archive_ids
        missing_in_main = archive_ids - main_ids

        if missing_in_archive:
            issues.append(f"Records missing in archive: {missing_in_archive}")

        if missing_in_main:
            issues.append(f"Records missing in main: {missing_in_main}")

        return issues


class SchemaTransferValidator:
    @staticmethod
    def validate_schema_transfer(db: Session, table_name: str, source_schema: str,
                                 target_schema: str, record_count: int) -> List[str]:
        issues = []

        source_count = db.execute(f"SELECT COUNT(*) FROM {source_schema}.{table_name}").scalar()
        target_count = db.execute(f"SELECT COUNT(*) FROM {target_schema}.{table_name}").scalar()

        if source_count > 0 and record_count == 0:
            issues.append(f"Expected to transfer {record_count} records, but source has {source_count} records")

        if target_count < record_count:
            issues.append(f"Target schema has {target_count} records, expected at least {record_count}")

        return issues

    @staticmethod
    def validate_data_quality(db: Session, table_name: str, schema: str,
                              checks: List[Dict[str, Any]]) -> List[str]:
        issues = []

        for check in checks:
            check_type = check.get("type")
            column = check.get("column")

            if check_type == "not_null":
                null_count = db.execute(
                    f"SELECT COUNT(*) FROM {schema}.{table_name} WHERE {column} IS NULL"
                ).scalar()
                if null_count > 0:
                    issues.append(f"Column {column} has {null_count} NULL values")

            elif check_type == "unique":
                total_count = db.execute(f"SELECT COUNT(*) FROM {schema}.{table_name}").scalar()
                distinct_count = db.execute(
                    f"SELECT COUNT(DISTINCT {column}) FROM {schema}.{table_name}"
                ).scalar()
                if total_count != distinct_count:
                    issues.append(f"Column {column} has duplicate values: {total_count} total, {distinct_count} distinct")

            elif check_type == "range":
                min_val = check.get("min")
                max_val = check.get("max")
                if min_val is not None:
                    below_min = db.execute(
                        f"SELECT COUNT(*) FROM {schema}.{table_name} WHERE {column} < {min_val}"
                    ).scalar()
                    if below_min > 0:
                        issues.append(f"Column {column} has {below_min} values below minimum {min_val}")
                if max_val is not None:
                    above_max = db.execute(
                        f"SELECT COUNT(*) FROM {schema}.{table_name} WHERE {column} > {max_val}"
                    ).scalar()
                    if above_max > 0:
                        issues.append(f"Column {column} has {above_max} values above maximum {max_val}")

        return issues


class ReferentialIntegrityChecker:
    @staticmethod
    def check_foreign_key_integrity(db: Session, table_name: str, schema: str,
                                    foreign_keys: List[Dict[str, str]]) -> List[str]:
        issues = []

        for fk in foreign_keys:
            column = fk["column"]
            ref_table = fk["ref_table"]
            ref_column = fk["ref_column"]
            ref_schema = fk.get("ref_schema", schema)

            orphan_count = db.execute(f"""
                SELECT COUNT(*)
                FROM {schema}.{table_name} t
                LEFT JOIN {ref_schema}.{ref_table} r ON t.{column} = r.{ref_column}
                WHERE r.{ref_column} IS NULL AND t.{column} IS NOT NULL
            """).scalar()

            if orphan_count > 0:
                issues.append(f"Table {schema}.{table_name} has {orphan_count} orphaned references in column {column} to {ref_schema}.{ref_table}.{ref_column}")

        return issues

    @staticmethod
    def validate_circular_references(db: Session, rides: List[MainRide],
                                     payments: List[MainPayment]) -> List[str]:
        issues = []

        ride_to_payment = {}
        payment_to_ride = {}

        for payment in payments:
            if payment.ride_id:
                payment_to_ride[str(payment.id)] = str(payment.ride_id)

        for ride in rides:
            ride_payments = [p for p in payments if str(p.ride_id) == str(ride.id)]
            if ride_payments:
                ride_to_payment[str(ride.id)] = [str(p.id) for p in ride_payments]

        for ride_id, payment_ids in ride_to_payment.items():
            for payment_id in payment_ids:
                if payment_id in payment_to_ride and payment_to_ride[payment_id] == ride_id:
                    continue

        return issues


def create_data_quality_report(validation_results: List[Dict[str, Any]]) -> str:
    report = ["Data Quality Validation Report", "=" * 50]

    total_checks = 0
    passed_checks = 0
    failed_checks = 0

    for result in validation_results:
        check_name = result.get("check_name", "Unknown Check")
        issues = result.get("issues", [])

        total_checks += 1
        if issues:
            failed_checks += 1
            report.append(f"\n{check_name}: FAILED")
            for issue in issues:
                report.append(f"   - {issue}")
        else:
            passed_checks += 1
            report.append(f"\n{check_name}: PASSED")

    report.append("\n" + "=" * 50)
    if total_checks > 0:
        report.append(f"Summary: {passed_checks}/{total_checks} checks passed ({passed_checks/total_checks*100:.1f}%)")

    if failed_checks > 0:
        report.append(f"{failed_checks} checks failed, review issues above")

    return "\n".join(report)