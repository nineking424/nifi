// FIle Name : parse-sqlmeta.groovy
// Type: Groovy Script
// Description: This script parses the XML content of the catalog file and extracts the query, schema, and arguments information.
// Author: Seoktae Jeong
// Version: 1.0
// Date: 2024-03-25
// Usage: This script is used in the ParseCatalog processor to parse the XML content of the catalog file and extract the query, schema, and arguments information.
// Reference:
// - https://community.cloudera.com/t5/Community-Articles/ExecuteScript-Cookbook-part-1/ta-p/248922
// - https://community.cloudera.com/t5/Community-Articles/ExecuteScript-Cookbook-part-2/ta-p/249018


import org.apache.nifi.processor.io.InputStreamCallback
import groovy.xml.XmlSlurper
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

// 타입명과 타입번호의 맵핑 정보를 정의합니다.
def typeMapping = [
    'LONGNVARCHAR':  '-16',
    'BIT':           '-7',
    'BOOLEAN':       '16',
    'TINYINT':       '-6',
    'BIGINT':        '-5',
    'LONGVARBINARY': '-4',
    'VARBINARY':     '-3',
    'BINARY':        '-2',
    'LONGVARCHAR':   '-1',
    'CHAR':          '1',
    'NUMERIC':       '2',
    'DECIMAL':       '3',
    'INTEGER':       '4',
    'SMALLINT':      '5',
    'FLOAT':         '6',
    'REAL':          '7',
    'DOUBLE':        '8',
    'VARCHAR':       '12',
    'DATE':          '91',
    'TIME':          '92',
    'TIMESTAMP':     '93',
    'VARCHAR':       '12',
    'CLOB':          '2005',
    'NCLOB':         '2011'
]

def flowFile = session.get()
if (!flowFile) return

def xmlContent = ''
session.read(flowFile, { inputStream ->
    xmlContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8)
} as InputStreamCallback)

def slurper = new XmlSlurper().parseText(xmlContent)
def attributes = [:]

def query = slurper.query?.text()
def schema = slurper.schema?.text()

if (query && schema) {
    attributes['sql.query'] = query
    attributes['sql.schema'] = schema

    def args = slurper.args?.arg
    if (args) {
        for (int i = 0; i < args.size(); i++) {
            def arg = args[i]
            attributes["sql.args.${i+1}.type"] = arg.type?.text() ?: '12'
            attributes["sql.args.${i+1}.value"] = arg.value?.text() ?: '-'
            
            // 타입명이 맵핑에 있는지 확인하고, 있으면 변경합니다.
            def type = attributes["sql.args.${i+1}.type"]
            if (typeMapping[type]) {
                attributes["sql.args.${i+1}.type"] = typeMapping[type]
            }
        }
    }

    flowFile = session.putAllAttributes(flowFile, attributes)
    session.transfer(flowFile, REL_SUCCESS)
} else {
    log.error("Missing essential elements (query or schema) in XML.")
    session.transfer(flowFile, REL_FAILURE)
}
