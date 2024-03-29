# extract-schema.py
# Author : Seoktae Jeong
# Summary : A script that generates an Avro schema by querying the table structure of an Oracle DB
# Usage : python extract-schema.py
# Note : The script loads Oracle DB connection information from the .env file. If there is no .env file, you need to set environment variables.
#        The .env file should contain the following information.
#        ORACLE_HOST=localhost
#        ORACLE_PORT=1521
#        ORACLE_SID=ORCL
#        ORACLE_USER=your_username
#        ORACLE_PASSWORD=your_password
#        TABLE_NAME=your_table_name
#        SCHEMA_NAME=your_schema_name

 
import cx_Oracle
import json
from dotenv import load_dotenv
import os

# .env 파일로부터 환경 변수 로드
load_dotenv()

# 환경 변수 로드
oracle_host = os.getenv("ORACLE_HOST", "localhost")
oracle_port = os.getenv("ORACLE_PORT", 1521)
oracle_sid = os.getenv("ORACLE_SID", "ORCL")
oracle_user = os.getenv("ORACLE_USER")
oracle_password = os.getenv("ORACLE_PASSWORD")
table_name = os.getenv("TABLE_NAME")
schema_name = os.getenv("SCHEMA_NAME")

# Oracle DB 접속 설정
dsn = cx_Oracle.makedsn(oracle_host, oracle_port, sid=oracle_sid)
connection = cx_Oracle.connect(user=oracle_user, password=oracle_password, dsn=dsn)

def oracle_type_to_avro(column_name, data_type, data_precision, data_scale, nullable, data_default):
    """
    Oracle 타입과 기본값을 Avro 타입으로 변환
    """
    avro_type = {"name": column_name.upper(), "type": []}

    # NULL 값 처리
    if nullable == "Y":
        avro_type["type"].append("null")

    # 데이터 타입 처리
    if data_type == "VARCHAR2" or data_type == "CHAR":
        avro_type["type"].append("string")
    elif data_type == "NUMBER":
        if data_scale > 0:
            avro_type["type"].append("double")
        elif data_precision is None or data_precision > 9:
            avro_type["type"].append("long")
        else:
            avro_type["type"].append("int")
    elif data_type == "DATE":
        # avro_type["type"].append({"type": "int", "logicalType": "date"})
        # date 타입은 시간 정보가 생략되므로 timestamp-millis로 처리 필요
        avro_type["type"].append({"type": "long", "logicalType": "timestamp-millis"})
    else:
        avro_type["type"].append("string")  # 처리되지 않은 타입은 기본적으로 string으로 처리

    # 필드 타입이 단일 선택지인 경우, 타입 리스트 대신 단일 타입 사용
    if len(avro_type["type"]) == 1:
        avro_type["type"] = avro_type["type"][0]
    
    # 기본값 처리
    if data_default is not None:
        # DATE 타입의 기본값 처리
        if data_type == "DATE":
            # DATE 기본값을 Avro 스키마에 맞게 변환하는 로직을 추가해야 합니다.
            # 이 예제에서는 단순화를 위해 생략합니다.
            pass
        else:
            avro_type["default"] = data_default
    elif "null" in avro_type["type"]:
        avro_type["default"] = None

    return avro_type

def generate_avro_schema_from_table(table_name, schema_name):
    """
    테이블 구조를 조회하여 Avro 스키마 생성, DATA_DEFAULT를 포함
    """
    avro_schema = {
        "type": "record",
        "name": table_name,
        "namespace": schema_name,
        "fields": []
    }

    query = """
    SELECT COLUMN_NAME, DATA_TYPE, DATA_PRECISION, DATA_SCALE, NULLABLE, DATA_DEFAULT
    FROM ALL_TAB_COLUMNS
    WHERE TABLE_NAME = :table_name AND OWNER = :schema_name
    ORDER BY COLUMN_ID
    """
    
    cursor = connection.cursor()
    cursor.execute(query, table_name=table_name.upper(), schema_name=schema_name.upper())
    
    for column_name, data_type, data_precision, data_scale, nullable, data_default in cursor:
        avro_field = oracle_type_to_avro(column_name, data_type, data_precision, data_scale, nullable, data_default)
        avro_schema["fields"].append(avro_field)
    
    cursor.close()
    return avro_schema

# 사용 예시
avro_schema = generate_avro_schema_from_table(table_name, schema_name)

# 생성된 Avro 스키마를 JSON으로 출력
print(json.dumps(avro_schema, indent=4))

# 생성된 Avro 스키마를 파일로 저장 및 파일이름을 스키마명.테이블명.avsc로 저장
with open(f"{schema_name}.{table_name}.avsc", "w") as f:
    json.dump(avro_schema, f, indent=4)
    print(f"Avro 스키마를 {schema_name}.{table_name}.avsc 파일로 저장했습니다.")

# 연결 종료
connection.close()
