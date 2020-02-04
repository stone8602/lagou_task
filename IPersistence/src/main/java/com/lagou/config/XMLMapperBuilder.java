package com.lagou.config;

import com.lagou.pojo.Configuration;
import com.lagou.pojo.MappedStatement;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.List;

public class XMLMapperBuilder {

    private Configuration configuration;

    public static final String TAG_SELECT="select";
    public static final String TAG_INSERT="insert";
    public static final String TAG_UPDATE="update";
    public static final String TAG_DELETE="delete";

    public XMLMapperBuilder(Configuration configuration) {
        this.configuration =configuration;
    }

    public void parse(InputStream inputStream) throws DocumentException {

        Document document = new SAXReader().read(inputStream);
        Element rootElement = document.getRootElement();

        String namespace = rootElement.attributeValue("namespace");

        List<Element> list = rootElement.selectNodes("//select");
        for (Element element : list) {
            buildMappedStatement(element,namespace,TAG_SELECT);
        }
        List<Element> insertList=rootElement.selectNodes("//insert");
        for(Element element:insertList){
            buildMappedStatement(element,namespace,TAG_INSERT);
        }
        List<Element> updateList=rootElement.selectNodes("//update");
        for(Element element:updateList){
            buildMappedStatement(element,namespace,TAG_UPDATE);
        }
        List<Element> deleteList=rootElement.selectNodes("//delete");
        for(Element element:deleteList){
            buildMappedStatement(element,namespace,TAG_DELETE);
        }
    }

    private void buildMappedStatement(Element element,String namespace,String tag){
        String id = element.attributeValue("id");
        String resultType = element.attributeValue("resultType");
        String paramterType = element.attributeValue("paramterType");
        String sqlText = element.getTextTrim();
        MappedStatement mappedStatement = new MappedStatement();
        mappedStatement.setId(id);
        mappedStatement.setResultType(resultType);
        mappedStatement.setParamterType(paramterType);
        mappedStatement.setSql(sqlText);
        mappedStatement.setTag(tag);
        String key = namespace+"."+id;
        configuration.getMappedStatementMap().put(key,mappedStatement);
    }
}
