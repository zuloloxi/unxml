package com.nerdforge.unxml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.PrivateModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.nerdforge.unxml.factory.ArrayNodeParserBuilderFactory;
import com.nerdforge.unxml.factory.ObjectNodeParserFactory;
import com.nerdforge.unxml.parsers.*;
import com.nerdforge.unxml.factory.ArrayNodeParserFactory;
import com.nerdforge.unxml.parsers.builders.ArrayNodeParserBuilder;
import com.nerdforge.unxml.xml.SimpleNamespaceContext;
import com.nerdforge.unxml.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.xerces.impl.Constants.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.Map;

public class UnXmlModule extends PrivateModule {
    Map<String, String> namespaces = new HashMap<>();

    public UnXmlModule(){}

    public UnXmlModule(Map<String, String> namespaces){
        this.namespaces = namespaces;
    }

    @Override
    protected void configure() {
        // Generate Factories
        install(new FactoryModuleBuilder()
                .implement(ObjectNodeParser.class, ObjectNodeParser.class)
                .build(ObjectNodeParserFactory.class));

        install(new FactoryModuleBuilder()
                .implement(ArrayNodeParser.class, ArrayNodeParser.class)
                .build(ArrayNodeParserFactory.class));

        install(new FactoryModuleBuilder()
                .implement(ArrayNodeParserBuilder.class, ArrayNodeParserBuilder.class)
                .build(ArrayNodeParserBuilderFactory.class));

        // Logger
        bind(Logger.class).toInstance(LoggerFactory.getLogger("unXml"));

        // JSON Mapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        bind(ObjectMapper.class).annotatedWith(Names.named("json-mapper")).toInstance(mapper);

        // bind xml namespaces
        bind(NamespaceContext.class).toInstance(new SimpleNamespaceContext(namespaces));

        // bind document builder factory
        bind(DocumentBuilderFactory.class).toInstance(documentBuilderFactory());

        bind(Parsing.class);
        expose(Parsing.class);

        bind(XmlUtil.class);
        expose(XmlUtil.class);

        bind(SimpleParsers.class);
        expose(SimpleParsers.class);
    }

    private static DocumentBuilderFactory documentBuilderFactory() {
        try {
            String name = "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl";
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(name, UnXmlModule.class.getClassLoader());
            factory.setFeature(SAX_FEATURE_PREFIX + EXTERNAL_GENERAL_ENTITIES_FEATURE, false);
            factory.setFeature(SAX_FEATURE_PREFIX + EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
            factory.setFeature(XERCES_FEATURE_PREFIX + DISALLOW_DOCTYPE_DECL_FEATURE, true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setNamespaceAware(true);

            return factory;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
