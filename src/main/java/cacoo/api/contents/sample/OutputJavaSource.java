package cacoo.api.contents.sample;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class OutputJavaSource {
    private static final String CLASS_STENCIL_ID = "00342";

    private static final Map<String, String> VISIBILITY_MAP;
    static {
        VISIBILITY_MAP = new HashMap<String, String>();
        VISIBILITY_MAP.put("+", "public ");
        VISIBILITY_MAP.put("#", "protected ");
        VISIBILITY_MAP.put("-", "private ");
        VISIBILITY_MAP.put("~", "");
    }

    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("([-+#~]) (\\S+) : (\\S+)");
    private static final Pattern METHOD_PATTERN =
        Pattern.compile("([-+#~]) ([^\\s]+)\\((?:(\\S+) : (\\S+))?\\) : (\\S+)");

    private String apiKey;
    private String diagramId;
    private String outputPath;

    public OutputJavaSource withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public OutputJavaSource withDiagramId(String diagramId) {
        this.diagramId = diagramId;
        return this;
    }

    public OutputJavaSource withOutputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public void execute() throws Exception {
        String url =
            String.format(
                "%s/api/v1/diagrams/%s/contents.xml?returnValues=uid,textStyle,shapeStyle&apiKey=%s",
                MainFrame.ROOT_PATH,
                diagramId,
                apiKey);
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
        XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList stencilNodes =
            (NodeList) xpath.evaluate(
                String.format("//group[@attr-stencil-id='%s']", CLASS_STENCIL_ID),
                document,
                XPathConstants.NODESET);
        for (int i = 0; i < stencilNodes.getLength(); i++) {
            Element stencilElement = (Element) stencilNodes.item(i);
            String uid = stencilElement.getAttribute("uid");
            String parentClassName = getParentClassName(uid, document);

            NodeList texts = stencilElement.getElementsByTagName("text");
            Element className = (Element) ((Element) texts.item(0)).getFirstChild();
            NodeList attributes = ((Element) texts.item(1)).getChildNodes();
            NodeList methods = ((Element) texts.item(2)).getChildNodes();

            outputFile(className, parentClassName, attributes, methods);
        }
    }

    private String getParentClassName(String uid, Document document) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        String parentUidExpression =
            String.format("//line[start/@connect-uid='%1$s' and end/@style='arrow(white)']/end/@connect-uid | "
                + "//line[end/@connect-uid='%1$s' and start/@style='arrow(white)']/start/@connect-uid", uid);
        NodeList attNodes = (NodeList) xpath.evaluate(parentUidExpression, document, XPathConstants.NODESET);

        if (attNodes.getLength() == 0) {
            return null;
        }

        String parentUid = ((Attr) attNodes.item(0)).getValue();
        String parentNameExpression =
            String.format(
                "//group[@attr-stencil-id='%s' and @uid='%s']/text[1]/textStyle/text()",
                CLASS_STENCIL_ID,
                parentUid);

        return xpath.evaluate(parentNameExpression, document);
    }

    private void outputFile(Element classElement, String parentClassName, NodeList attNodes, NodeList methodNodes)
            throws IOException {
        String className = classElement.getTextContent();

        PrintWriter writer = new PrintWriter(outputPath + File.separator + className + ".java");
        writer.print("public ");
        writer.print(getAbstractStr(classElement));
        writer.print("class ");
        writer.print(className);
        if (parentClassName != null && parentClassName.length() > 0) {
            writer.print(" extends ");
            writer.print(parentClassName);
        }
        writer.println(" {");

        for (int i = 0; i < attNodes.getLength(); i++) {
            Element e = (Element) attNodes.item(i);
            String staticStr = getStaticStr(e);

            for (String att : e.getTextContent().split("\n")) {
                Matcher m = ATTRIBUTE_PATTERN.matcher(att);
                if (!m.matches()) {
                    System.out.println("attribute parse error " + att);
                    continue;
                }
                writer.print("\t");
                writer.print(VISIBILITY_MAP.get(m.group(1)));
                writer.print(staticStr);
                writer.print(m.group(3));
                writer.print(" ");
                writer.print(m.group(2));
                writer.println(";");
            }
        }

        writer.println("");

        for (int i = 0; i < methodNodes.getLength(); i++) {
            Element e = (Element) methodNodes.item(i);
            String abstractStr = getAbstractStr(e);
            String staticStr = getStaticStr(e);

            for (String text : e.getTextContent().split("\n")) {
                Matcher m = METHOD_PATTERN.matcher(text);
                if (!m.matches()) {
                    System.out.println("method parse error");
                    continue;
                }
                writer.print("\t");
                writer.print(VISIBILITY_MAP.get(m.group(1)));
                writer.print(abstractStr);
                writer.print(staticStr);
                writer.print(m.group(5));
                writer.print(" ");
                writer.print(m.group(2));
                writer.print("(");
                if (m.group(3) != null) {
                    writer.print(m.group(4));
                    writer.print(" ");
                    writer.print(m.group(3));
                }
                if (abstractStr.length() > 0) {
                    writer.println(");");
                } else {
                    writer.println(") {");
                    if (!m.group(5).equals("void")) {
                        writer.print("\t\treturn ");
                        writer.println(Character.isUpperCase(m.group(5).charAt(0)) ? "null;" : "0;");
                    }
                    writer.println("\t}");
                }
            }
        }

        writer.println("}");
        writer.close();
    }

    private static String getAbstractStr(Element element) {
        return "true".equals(element.getAttribute("italic")) ? "abstract " : "";
    }

    private static String getStaticStr(Element element) {
        return "true".equals(element.getAttribute("underline")) ? "static " : "";
    }
}
