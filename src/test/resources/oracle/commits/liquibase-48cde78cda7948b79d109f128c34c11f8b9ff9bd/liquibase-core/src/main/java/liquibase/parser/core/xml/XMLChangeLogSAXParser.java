package liquibase.parser.core.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.exception.ChangeLogParseException;
import liquibase.logging.LogFactory;
import liquibase.parser.ChangeLogParser;
import liquibase.resource.UtfBomStripperInputStream;
import liquibase.resource.ResourceAccessor;
import liquibase.util.file.FilenameUtils;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public class XMLChangeLogSAXParser implements ChangeLogParser {

    private SAXParserFactory saxParserFactory;

    public XMLChangeLogSAXParser() {
        saxParserFactory = SAXParserFactory.newInstance();

        if (System.getProperty("java.vm.version").startsWith("1.4")) {
            saxParserFactory.setValidating(false);
            saxParserFactory.setNamespaceAware(false);
        } else {
            saxParserFactory.setValidating(true);
            saxParserFactory.setNamespaceAware(true);
        }
    }

    @Override
    public int getPriority() {
        return PRIORITY_DEFAULT;
    }

    public static String getSchemaVersion() {
        return "3.1";
    }

    @Override
    public boolean supports(String changeLogFile, ResourceAccessor resourceAccessor) {
        return changeLogFile.endsWith("xml");
    }

    @Override
    public DatabaseChangeLog parse(String physicalChangeLogLocation, ChangeLogParameters changeLogParameters, ResourceAccessor resourceAccessor, LiquibaseConfiguration context) throws ChangeLogParseException {

        InputStream inputStream = null;
        try {
            SAXParser parser = saxParserFactory.newSAXParser();
            try {
                parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
            } catch (SAXNotRecognizedException e) {
                //ok, parser must not support it
            } catch (SAXNotSupportedException e) {
                //ok, parser must not support it
            }

            XMLReader xmlReader = parser.getXMLReader();
            LiquibaseEntityResolver resolver=new LiquibaseEntityResolver(this);
            resolver.useResoureAccessor(resourceAccessor,FilenameUtils.getFullPath(physicalChangeLogLocation));
            xmlReader.setEntityResolver(resolver);
            xmlReader.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    LogFactory.getLogger().warning(exception.getMessage());
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    LogFactory.getLogger().severe(exception.getMessage());
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    LogFactory.getLogger().severe(exception.getMessage());
                    throw exception;
                }
            });
        	
            inputStream = resourceAccessor.getResourceAsStream(physicalChangeLogLocation);
            if (inputStream == null) {
                throw new ChangeLogParseException(physicalChangeLogLocation + " does not exist");
            }

            XMLChangeLogSAXHandler contentHandler = new XMLChangeLogSAXHandler(physicalChangeLogLocation, resourceAccessor, changeLogParameters, context);
            xmlReader.setContentHandler(contentHandler);
            xmlReader.parse(new InputSource(new UtfBomStripperInputStream(inputStream)));

            return contentHandler.getDatabaseChangeLog();
        } catch (ChangeLogParseException e) {
            throw e;
        } catch (IOException e) {
            throw new ChangeLogParseException("Error Reading Migration File: " + e.getMessage(), e);
        } catch (SAXParseException e) {
            throw new ChangeLogParseException("Error parsing line " + e.getLineNumber() + " column " + e.getColumnNumber() + " of " + physicalChangeLogLocation +": " + e.getMessage(), e);
        } catch (SAXException e) {
            Throwable parentCause = e.getException();
            while (parentCause != null) {
                if (parentCause instanceof ChangeLogParseException) {
                    throw ((ChangeLogParseException) parentCause);
                }
                parentCause = parentCause.getCause();
            }
            String reason = e.getMessage();
            String causeReason = null;
            if (e.getCause() != null) {
                causeReason = e.getCause().getMessage();
            }

//            if (reason == null && causeReason==null) {
//                reason = "Unknown Reason";
//            }
            if (reason == null) {
                if (causeReason != null) {
                    reason = causeReason;
                } else {
                    reason = "Unknown Reason";
                }
            }

            throw new ChangeLogParseException("Invalid Migration File: " + reason, e);
        } catch (Exception e) {
            throw new ChangeLogParseException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // probably ok
                }
            }
        }
    }
}
