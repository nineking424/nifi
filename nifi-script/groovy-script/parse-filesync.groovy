// FIle Name : parse-filesync.groovy
// Type: Groovy Script
// Description: This script parses the sync file and extracts the source and destination paths.
// Author: Seoktae Jeong
// Version: 1.0
// Date: 2024-03-30
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.commons.io.IOUtils;
import java.nio.charset.StandardCharsets;
import java.io.File;

def flowFile = session.get();
if (!flowFile) return;

def content = '';
session.read(flowFile, { inputStream ->
    content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
} as InputStreamCallback);

def startSrc = '/dft_svc';
def endSrc = 'mdf';
def startDst = '/DataLake';
def endDst = 'mdf';

def tokens = content.tokenize('`');
def attributes = [:];
def src = '';
def dst = '';

// tokens 배열을 for문으로 순회
for (int i = 0; i < tokens.size(); i++) {
    if (tokens[i].startsWith(startSrc) && tokens[i].endsWith(endSrc)) {
        src = tokens[i];
    } else if (tokens[i].startsWith(startDst) && tokens[i].endsWith(endDst)) {
        dst = tokens[i];
    }
}

// src와 dst를 찾지 못했을 경우 에러 처리
if(src.isEmpty() || dst.isEmpty()) {
    log.error("Could not find the specified start and/or end strings in the tokens.");
    session.transfer(flowFile, REL_FAILURE);
    return;
}

// src와 dst의 경로 및 파일명 추출
def srcFile = new File(src);
def dstFile = new File(dst);

// attributes에 경로와 파일명 추가
attributes['src.fullpath'] = src;
attributes['src.path'] = srcFile.parent;
attributes['src.filename'] = srcFile.name;
attributes['dst.path'] = dstFile.parent;
attributes['dst.filename'] = dstFile.name;
attributes['dst.fullpath'] = dst;

flowFile = session.putAllAttributes(flowFile, attributes);
session.transfer(flowFile, REL_SUCCESS);
