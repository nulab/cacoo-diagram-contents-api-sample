package cacoo.api.contents.sample;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OutputHtmlSource {

    private String apiKey;
    private String diagramId;
    private String outputPath;

    private Set<String> imageSources = new HashSet<String>();

    private Map<String, HtmlConverter> elementConverterMap = new HashMap<String, HtmlConverter>();
    private Map<String, HtmlConverter> stencilConverterMap = new HashMap<String, HtmlConverter>();

    public OutputHtmlSource() {
        initMaps();
    }

    public OutputHtmlSource withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public OutputHtmlSource withDiagramId(String diagramId) {
        this.diagramId = diagramId;
        return this;
    }

    public OutputHtmlSource withOutputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public void execute() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        String url =
            String.format(
                "%s/api/v1/diagrams/%s/contents.xml?returnValues=position,textStyle,shapeStyle,uid,point&apiKey=%s",
                MainFrame.ROOT_PATH,
                diagramId,
                apiKey);
        Document document = builder.parse(url);

        NodeList sheetNodes = (NodeList) document.getDocumentElement().getChildNodes();

        BackgroundSheetGetter bgsheet = new BackgroundSheetGetter(sheetNodes);

        for (int i = 0; i < sheetNodes.getLength(); i++) {
            Element sheetElement = (Element) sheetNodes.item(i);
            String uid = sheetElement.getAttribute("uid");
            if (bgsheet.isBackgroundSheet(uid)) {
                continue;
            }

            Document htmlDoc = builder.newDocument();

            Element head = htmlDoc.createElement("head");
            Element title = htmlDoc.createElement("title");
            title.appendChild(htmlDoc.createTextNode(sheetElement.getAttribute("name")));
            head.appendChild(title);

            Element body = htmlDoc.createElement("body");
            body.setAttribute("style", "position:relative;margin:0;padding:0");
            DocumentFragment contents = htmlDoc.createDocumentFragment();
            for (Node node : bgsheet.getBackgroundHtmlList(uid)) {
                contents.appendChild(htmlDoc.importNode(node, true));
            }
            contents.appendChild(convertShapes(sheetElement.getChildNodes(), htmlDoc));
            body.appendChild(contents);

            Element html = htmlDoc.createElement("html");
            html.appendChild(head);
            html.appendChild(body);
            htmlDoc.appendChild(html);

            String fileName = outputPath + File.separator + sheetElement.getAttribute("name") + ".html";
            transformer.transform(new DOMSource(htmlDoc), new StreamResult(new File(fileName)));
            
        }

        // download images
        for (String sourceId : imageSources) {
            URL imageUrl =
                new URL(String.format(
                    "%s/api/v1/diagrams/%s/contents/images/%s?apiKey=%s",
                    MainFrame.ROOT_PATH,
                    diagramId,
                    sourceId,
                    apiKey));

            InputStream is = null;
            OutputStream os = null;
            try {
                is = imageUrl.openConnection().getInputStream();
                os = new FileOutputStream(outputPath + File.separator + sourceId);
                IOUtils.copy(is, os);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
        }
    }

    class BackgroundSheetGetter {
        private Map<String, String> backgroundSheetMap = new HashMap<String, String>();
        private Map<String, Node> backgroundHtmlMap = new HashMap<String, Node>();

        BackgroundSheetGetter(NodeList sheetNodes) throws ParserConfigurationException {
            Document backDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            Map<String, Element> uidElementMap = new HashMap<String, Element>();
            Set<String> bgSheets = new HashSet<String>();

            for (int i = 0; i < sheetNodes.getLength(); i++) {
                Element e = (Element) sheetNodes.item(i);
                if (e.hasAttribute("background-sheet")) {
                    backgroundSheetMap.put(e.getAttribute("uid"), e.getAttribute("background-sheet"));
                    bgSheets.add(e.getAttribute("background-sheet"));
                }
                uidElementMap.put(e.getAttribute("uid"), e);
            }

            for (String bgsheet : bgSheets) {
                Node node = convertShapes(uidElementMap.get(bgsheet).getChildNodes(), backDoc);
                backgroundHtmlMap.put(bgsheet, node);
            }
        }

        List<Node> getBackgroundHtmlList(String uid) {
            List<Node> nodes = new ArrayList<Node>();
            while (backgroundSheetMap.containsKey(uid)) {
                String bgUid = backgroundSheetMap.get(uid);
                nodes.add(backgroundHtmlMap.get(bgUid));
                uid = bgUid;
            }
            Collections.reverse(nodes);
            return nodes;
        }

        boolean isBackgroundSheet(String uid) {
            return backgroundHtmlMap.containsKey(uid);
        }
    }

    private Node convertShapes(NodeList nodes, Document document) {
        DocumentFragment node = document.createDocumentFragment();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String name = element.getNodeName();
            if (elementConverterMap.containsKey(name)) {
                HtmlConverter converter = elementConverterMap.get(name);
                node.appendChild(converter.convert(element, document));
            }
        }
        return node;
    }

    private Node convertTexts(Element element, Document document) {
        DocumentFragment htmlElement = document.createDocumentFragment();
        if (element.getTextContent().length() == 0) {
            return htmlElement;
        }

        NodeList nodes = element.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element textStyle = (Element) nodes.item(i);

            Map<String, String> map = new java.util.LinkedHashMap<String, String>();
            map.put("color", textStyle.getAttribute("color"));
            map.put("font-family", textStyle.getAttribute("font"));
            map.put("font-size", textStyle.getAttribute("size"));
            map.put("font-style", "true".equals(textStyle.getAttribute("italic")) ? "italic" : "normal");
            map.put("font-weight", textStyle.getAttribute("weight"));
            map.put("text-decoration", "true".equals(textStyle.getAttribute("underline")) ? "underline;" : "none;");

            Element span = document.createElement("span");
            span.setAttribute("style", toStyleStr(map));
            span.appendChild(document.createTextNode(textStyle.getTextContent()));

            htmlElement.appendChild(span);
        }

        return htmlElement;
    }

    private Node convertHeader(String tag, Element element, Document document) {
        Element htmlElement = document.createElement(tag);
        htmlElement.setAttribute("style", toStyleStr(getPositionStyle(element)));
        htmlElement.appendChild(convertTexts((Element) element.getFirstChild(), document));
        return htmlElement;
    }

    private void initMaps() {
        elementConverterMap.put("group", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                String stencilId = element.getAttribute("attr-stencil-id");
                if (stencilConverterMap.containsKey(stencilId)) {
                    return stencilConverterMap.get(stencilId).convert(element, document);
                }

                Element htmlElement = document.createElement("div");
                htmlElement.setAttribute("style", toStyleStr(getPositionStyle(element)));
                htmlElement.appendChild(convertShapes(element.getChildNodes(), document));
                return htmlElement;
            }
        });
        elementConverterMap.put("image", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                if (!element.hasAttribute("source-id")) {
                    return document.createDocumentFragment();
                }
                String sourceId = element.getAttribute("source-id");
                if (!imageSources.contains(sourceId)) {
                    imageSources.add(sourceId);
                }

                Element htmlElement = document.createElement("img");
                htmlElement.setAttribute("src", sourceId);
                htmlElement.setAttribute("style", toStyleStr(getPositionStyle(element)));
                return htmlElement;
            }
        });
        elementConverterMap.put("text", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                String text = element.getFirstChild().getTextContent();
                if (text.length() == 0) {
                    return document.createDocumentFragment();
                }

                Element htmlElement = document.createElement("div");
                htmlElement.setAttribute("style", toStyleStr(getPositionStyle(element)) + "; display: inline-table;");
                htmlElement.appendChild(getTextDiv(element, document));
                return htmlElement;
            }
        });
        elementConverterMap.put("line", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                Element canvas = document.createElement("canvas");
                canvas.setAttribute("id", element.getAttribute("uid"));
                canvas.setAttribute("style", toStyleStr(getPositionStyle(element)));
                canvas.setAttribute("width", element.getAttribute("width"));
                canvas.setAttribute("height", element.getAttribute("height"));

                Element js = document.createElement("script");
                js.setAttribute("type", "text/javascript");
                StringBuilder sb = new StringBuilder();
                sb.append("var canvas = document.getElementById('").append(element.getAttribute("uid")).append("');\n");
                sb.append("var ctx = canvas.getContext('2d');\n");
                sb.append("ctx.beginPath();\n");
                sb.append("ctx.strokeStyle = '").append(element.getAttribute("border-color")).append("';\n");
                sb.append("ctx.lineWidth = ").append(element.getAttribute("border-thickness")).append(";\n");

                NodeList points = element.getElementsByTagName("point");

                int n = 0;
                while (n < points.getLength()) {
                    Element point = (Element) points.item(n);
                    String x = point.getAttribute("x");
                    String y = point.getAttribute("y");
                    if (n == 0) {
                        sb.append("ctx.moveTo(").append(x).append(",").append(y).append(")\n");
                    } else if ("BEZIER_CP".equals(point.getAttribute("type"))) {
                        n++;
                        Element np = (Element) points.item(n);
                        String nx = np.getAttribute("x");
                        String ny = np.getAttribute("y");
                        sb
                            .append("ctx.quadraticCurveTo(")
                            .append(x)
                            .append(",")
                            .append(y)
                            .append(",")
                            .append(nx)
                            .append(",")
                            .append(ny)
                            .append(");\n");
                    } else {
                        sb.append("ctx.lineTo(").append(x).append(",").append(y).append(");\n");
                    }
                    n++;
                }
                sb.append("ctx.stroke();\n");
                js.setTextContent(sb.toString());

                DocumentFragment htmlElement = document.createDocumentFragment();
                htmlElement.appendChild(canvas);
                htmlElement.appendChild(js);

                return htmlElement;
            }
        });
        elementConverterMap.put("polygon", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                Element canvas = document.createElement("canvas");
                canvas.setAttribute("id", element.getAttribute("uid"));
                canvas.setAttribute("style", toStyleStr(getPositionStyle(element)));
                canvas.setAttribute("width", element.getAttribute("width"));
                canvas.setAttribute("height", element.getAttribute("height"));

                Element js = document.createElement("script");
                js.setAttribute("type", "text/javascript");
                StringBuilder sb = new StringBuilder();
                sb.append("var canvas = document.getElementById('").append(element.getAttribute("uid")).append("');\n");
                sb.append("var ctx = canvas.getContext('2d');\n");
                boolean stroke = false;
                if (element.hasAttribute("border-color")) {
                    stroke = true;
                    sb.append("ctx.strokeStyle = '").append(element.getAttribute("border-color")).append("';\n");
                    sb.append("ctx.lineWidth = ").append(element.getAttribute("border-thickness")).append(";\n");
                }
                boolean fill = false;
                if (element.hasAttribute("fill-color")) {
                    fill = true;
                    sb.append("ctx.fillStyle = '").append(element.getAttribute("fill-color")).append("';\n");
                }
                NodeList paths = element.getElementsByTagName("path");

                for (int i = 0; i < paths.getLength(); i++) {
                    sb.append("ctx.beginPath();\n");
                    Element path = (Element) paths.item(i);
                    NodeList points = path.getElementsByTagName("point");
                    int n = 0;
                    while (n < points.getLength()) {
                        Element point = (Element) points.item(n);
                        String x = point.getAttribute("x");
                        String y = point.getAttribute("y");
                        if (n == 0) {
                            sb.append("ctx.moveTo(").append(x).append(",").append(y).append(")\n");
                        } else if ("BEZIER_CP".equals(point.getAttribute("type"))) {
                            n++;
                            Element np = (Element) points.item(n);
                            String nx = np.getAttribute("x");
                            String ny = np.getAttribute("y");
                            sb
                                .append("ctx.quadraticCurveTo(")
                                .append(x)
                                .append(",")
                                .append(y)
                                .append(",")
                                .append(nx)
                                .append(",")
                                .append(ny)
                                .append(");\n");
                        } else {
                            sb.append("ctx.lineTo(").append(x).append(",").append(y).append(");\n");
                        }
                        n++;
                    }

                    if ("true".equals(path.getAttribute("close"))) {
                        sb.append("ctx.closePath();\n");
                    }
                    if (fill) {
                        sb.append("ctx.fill();\n");
                    }
                    if (stroke) {
                        sb.append("ctx.stroke();\n");
                    }
                }
                js.setTextContent(sb.toString());

                DocumentFragment htmlElement = document.createDocumentFragment();
                htmlElement.appendChild(canvas);
                htmlElement.appendChild(js);

                return htmlElement;
            }

        });

        stencilConverterMap.put("00001", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                NodeList nodes = element.getChildNodes();
                Element polygon = (Element) nodes.item(0);

                Map<String, String> map = getPositionStyle(element);
                map.put("background-color", polygon.getAttribute("fill-color"));
                if (polygon.hasAttribute("border-style")) {
                    String style = polygon.getAttribute("border-style");
                    String cssStyle = "normal".equals(style) ? "solid" : style.substring(0, style.length() - 1);
                    map.put("border-style", cssStyle);
                    map.put("border-color", polygon.getAttribute("border-color"));
                    map.put("border-width", polygon.getAttribute("border-thickness"));
                }
                Element htmlElement = document.createElement("div");
                htmlElement.setAttribute("style", toStyleStr(map) + "display: inline-table;");
                htmlElement.appendChild(getTextDiv((Element) nodes.item(1), document));
                return htmlElement;
            }
        });
        stencilConverterMap.put("00257", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                Element htmlElement = document.createElement("a");
                htmlElement.setAttribute("style", toStyleStr(getPositionStyle(element)));
                htmlElement.setAttribute("href", "#");
                htmlElement.appendChild(convertTexts((Element) element.getFirstChild(), document));
                return htmlElement;
            }
        });
        stencilConverterMap.put("00273", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                return convertHeader("h1", element, document);
            }
        });
        stencilConverterMap.put("00274", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                return convertHeader("h2", element, document);
            }
        });
        stencilConverterMap.put("00275", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                return convertHeader("h3", element, document);
            }
        });
        stencilConverterMap.put("00276", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                return convertHeader("h4", element, document);
            }
        });
        stencilConverterMap.put("00277", new HtmlConverter() {
            @Override
            public Node convert(Element element, Document document) {
                return convertHeader("h5", element, document);
            }
        });
    }

    private Node getTextDiv(Element element, Document document) {
        String vAlign = element.getAttribute("v-align");

        Map<String, String> style = new LinkedHashMap<String, String>();
        style.put("display", "table-cell");
        style.put("text-align", element.getAttribute("h-align"));
        style.put("vertical-align", "center".equals(vAlign) ? "middle" : vAlign);

        Element htmlElement = document.createElement("div");
        htmlElement.setAttribute("style", toStyleStr(style));
        htmlElement.appendChild(convertTexts(element, document));
        return htmlElement;
    }

    private static Map<String, String> getPositionStyle(Element element) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("position", "absolute");
        map.put("margin", "0");
        map.put("padding", "0");
        map.put("left", element.getAttribute("x") + "px");
        map.put("top", element.getAttribute("y") + "px");
        map.put("width", element.getAttribute("width") + "px");
        map.put("height", element.getAttribute("height") + "px");
        return map;
    }

    private static String toStyleStr(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (String name : map.keySet()) {
            sb.append(name);
            sb.append(": ");
            sb.append(map.get(name));
            sb.append("; ");
        }
        return sb.toString();
    }

    interface HtmlConverter {
        Node convert(Element element, Document document);
    }

}
