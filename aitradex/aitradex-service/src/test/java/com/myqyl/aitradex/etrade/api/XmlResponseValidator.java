package com.myqyl.aitradex.etrade.api;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Utility for validating E*TRADE XML responses.
 */
public class XmlResponseValidator {

  private static final Logger log = LoggerFactory.getLogger(XmlResponseValidator.class);
  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  static {
    factory.setNamespaceAware(true);
  }

  /**
   * Parses XML string into a Document.
   */
  public static Document parseXml(String xml) {
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(new InputSource(new StringReader(xml)));
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse XML", e);
    }
  }

  /**
   * Validates that the root element matches the expected name.
   */
  public static void validateRootElement(Document doc, String expectedRootName) {
    Element root = doc.getDocumentElement();
    if (root == null || !root.getNodeName().equals(expectedRootName)) {
      throw new AssertionError(
          "Expected root element '" + expectedRootName + "', but got '" + 
          (root != null ? root.getNodeName() : "null") + "'");
    }
  }

  /**
   * Gets the first child element with the given name.
   */
  public static Element getFirstChildElement(Element parent, String childName) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node instanceof Element && node.getNodeName().equals(childName)) {
        return (Element) node;
      }
    }
    return null;
  }

  /**
   * Gets all child elements with the given name.
   */
  public static List<Element> getChildElements(Element parent, String childName) {
    List<Element> elements = new ArrayList<>();
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node instanceof Element && node.getNodeName().equals(childName)) {
        elements.add((Element) node);
      }
    }
    return elements;
  }

  /**
   * Gets text content of an element, or null if element doesn't exist.
   */
  public static String getTextContent(Element parent, String childName) {
    Element child = getFirstChildElement(parent, childName);
    return child != null ? child.getTextContent() : null;
  }

  /**
   * Validates that an element exists and has non-empty text content.
   */
  public static void validateRequiredField(Element parent, String fieldName) {
    String value = getTextContent(parent, fieldName);
    if (value == null || value.trim().isEmpty()) {
      throw new AssertionError("Required field '" + fieldName + "' is missing or empty");
    }
  }

  /**
   * Validates that an element exists and has non-empty text content, returning the value.
   */
  public static String getRequiredField(Element parent, String fieldName) {
    validateRequiredField(parent, fieldName);
    return getTextContent(parent, fieldName);
  }

  /**
   * Validates that a numeric field exists and can be parsed.
   */
  public static double getNumericField(Element parent, String fieldName) {
    String value = getTextContent(parent, fieldName);
    if (value == null || value.trim().isEmpty()) {
      throw new AssertionError("Numeric field '" + fieldName + "' is missing or empty");
    }
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      throw new AssertionError("Field '" + fieldName + "' is not a valid number: " + value);
    }
  }

  /**
   * Validates that a numeric field exists and can be parsed, or returns default if missing.
   */
  public static double getNumericFieldOrDefault(Element parent, String fieldName, double defaultValue) {
    String value = getTextContent(parent, fieldName);
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Validates that a URL field looks like a valid E*TRADE API URL.
   */
  public static void validateUrlField(Element parent, String fieldName) {
    String url = getTextContent(parent, fieldName);
    if (url != null && !url.isEmpty()) {
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        throw new AssertionError("Field '" + fieldName + "' does not look like a valid URL: " + url);
      }
      if (!url.contains("etrade.com") && !url.contains("/v1/")) {
        log.warn("URL field '{}' may not be a valid E*TRADE API URL: {}", fieldName, url);
      }
    }
  }
}
