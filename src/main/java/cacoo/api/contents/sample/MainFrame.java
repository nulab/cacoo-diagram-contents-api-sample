package cacoo.api.contents.sample;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public static final String ROOT_PATH = "https://cacoo.com";

    private static final String OUTPUT_JAVA_SAMPLE_ID = "fiNXi7g3cquoaLMu";
    private static final String OUTPUT_HTML_SAMPLE_ID = "uYLmRZomNIc0ngG5";
    private static final String SETUP_EC2_SAMPLE_ID = "p34VEcoIoROfDM4k";

    private JTextField apiKeyText = new JTextField();

    private JTextField outputJavaDiagramUrl = new JTextField(toDiagramUrl(OUTPUT_JAVA_SAMPLE_ID));
    private JTextField outputJavaDirText = new JTextField(System.getProperty("user.home"));

    private JTextField outputHtmlDiagramUrl = new JTextField(toDiagramUrl(OUTPUT_HTML_SAMPLE_ID));
    private JTextField outputHtmlDirText = new JTextField(System.getProperty("user.home"));

    private JTextField setupEc2DiagramUrl = new JTextField(toDiagramUrl(SETUP_EC2_SAMPLE_ID));
    private JTextField accessKeyText = new JTextField();
    private JTextField secretKeyText = new JTextField();
    private JTextField endpointText = new JTextField("ec2.ap-northeast-1.amazonaws.com");
    private JTextField keyNameText = new JTextField();
    private JTextField zoneText = new JTextField("ap-northeast-1a");
    private JTextField securityGroupText = new JTextField("default");

    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setVisible(true);
    }

    MainFrame() {
        setTitle("Cacoo contents API sample");
        setSize(950, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        add(createCommonPanel());
        add(createOutputJavaPanel());
        add(createOutputHtmlPanel());
        add(createSetupEC2Panel());
    }

    private Component createCommonPanel() {
        JEditorPane pane =
            createTextAndLink("Required if you want to use private diagrams.  ", "https://cacoo.com/profile/api", 380);

        JComponent[][] compos = { { new JLabel("Api Key"), apiKeyText, pane } };
        JPanel panel = createGroupLayoutPanel(compos);
        panel.setBorder(createTitledBorder("Common Setting", false));
        return panel;
    }

    private Component createOutputJavaPanel() {
        final JComponent[][] compos =
            {
                {
                    new JLabel("Diagram URL"),
                    outputJavaDiagramUrl,
                    createCopyButton(OUTPUT_JAVA_SAMPLE_ID, outputJavaDiagramUrl),
                    createLink(outputJavaDiagramUrl.getText()) },
                { new JLabel("Output Directory"), outputJavaDirText, createDirSelectButton(outputJavaDirText), null } };

        JButton button = new JButton(new AbstractAction("Output Java Source") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (!isValid(compos)) {
                    return;
                }

                try {
                    new OutputJavaSource().withApiKey(apiKeyText.getText()).withDiagramId(
                        getDiagramId(outputJavaDiagramUrl)).withOutputPath(outputJavaDirText.getText()).execute();
                    JOptionPane.showMessageDialog(MainFrame.this, "Succeeded.");
                } catch (Exception e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(MainFrame.this, "Faild.");
                }
            }
        });

        return createExecPanel("Output Java Source", compos, button, false);
    }

    private Component createOutputHtmlPanel() {
        final JComponent[][] compos =
            {
                {
                    new JLabel("Diagram URL"),
                    outputHtmlDiagramUrl,
                    createCopyButton(OUTPUT_HTML_SAMPLE_ID, outputHtmlDiagramUrl),
                    createLink(outputHtmlDiagramUrl.getText()) },
                { new JLabel("Output Directory"), outputHtmlDirText, createDirSelectButton(outputHtmlDirText), null } };

        JButton button = new JButton(new AbstractAction("Output Html Source") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (!isValid(compos)) {
                    return;
                }

                try {
                    new OutputHtmlSource().withApiKey(apiKeyText.getText()).withDiagramId(
                        getDiagramId(outputHtmlDiagramUrl)).withOutputPath(outputHtmlDirText.getText()).execute();
                    JOptionPane.showMessageDialog(MainFrame.this, "Succeeded.");
                } catch (Exception e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(MainFrame.this, "Faild.");
                }
            }
        });
        return createExecPanel("Output HTML Source", compos, button, false);
    }

    private Component createSetupEC2Panel() {
        final JComponent[][] compos =
            {
                {
                    new JLabel("Diagram URL"),
                    setupEc2DiagramUrl,
                    createCopyButton(SETUP_EC2_SAMPLE_ID, setupEc2DiagramUrl),
                    createLink(setupEc2DiagramUrl.getText()) },
                {
                    new JLabel("Access Key"),
                    accessKeyText,
                    null,
                    createLink("https://portal.aws.amazon.com/gp/aws/securityCredentials") },
                { new JLabel("Secret Key"), secretKeyText, null, null },
                { new JLabel("Endpoint"), endpointText, null, createLink("http://docs.amazonwebservices.com/general/latest/gr/rande.html#ec2_region") },
                { new JLabel("Key Pair Name"), keyNameText, null, null },
                { new JLabel("Zone"), zoneText, null, null },
                { new JLabel("Security Group"), securityGroupText, null, null } };

        JButton button = new JButton(new AbstractAction("Setup EC2") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (!isValid(compos)) {
                    return;
                }

                try {
                    new SetupEC2()
                        .withApiKey(apiKeyText.getText())
                        .withDiagramId(getDiagramId(setupEc2DiagramUrl))
                        .withAccessKey(accessKeyText.getText())
                        .withSecretKey(secretKeyText.getText())
                        .withEndpoint(endpointText.getText())
                        .withKeyName(keyNameText.getText())
                        .withZone(zoneText.getText())
                        .withSecurityGroup(securityGroupText.getText())
                        .execute();
                    JOptionPane.showMessageDialog(MainFrame.this, "Succeeded.");
                } catch (Exception e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(MainFrame.this, "Faild.");
                }
            }
        });

        return createExecPanel("Setup EC2", compos, button, true);
    }

    private JButton createCopyButton(final String diagramId, final JTextField text) {
        return new JButton(new AbstractAction("Copy") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(
                    MainFrame.this,
                    "Are you sure you want to copy this sample diagram on cacoo?",
                    "Copy Diagram",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                    return;
                }
                String url =
                    String.format("%s/api/v1/diagrams/%s/copy.xml?apiKey=%s", ROOT_PATH, diagramId, apiKeyText
                        .getText());
                Document document;
                try {
                    document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
                    XPath xpath = XPathFactory.newInstance().newXPath();
                    String stencilNodes = (String) xpath.evaluate("/diagram/url", document, XPathConstants.STRING);
                    text.setText(stencilNodes);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(MainFrame.this, "Faild.\nPlease check Api Key.");
                }
            }
        });
    }

    private JButton createDirSelectButton(final JTextField text) {
        return new JButton(new AbstractAction("Select...") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    text.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
    }

    private JPanel createExecPanel(String title, JComponent[][] compos, JComponent button, boolean last) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createTitledBorder(title, last));
        panel.add(createGroupLayoutPanel(compos), BorderLayout.NORTH);
        panel.add(button, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createGroupLayoutPanel(JComponent[][] compos) {
        JPanel panel = new JPanel();

        GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        panel.setLayout(layout);

        int ny = compos.length;
        int nx = compos[0].length;

        SequentialGroup hg = layout.createSequentialGroup();
        for (int x = 0; x < nx; x++) {
            ParallelGroup pg = layout.createParallelGroup();
            for (int y = 0; y < ny; y++) {
                JComponent c = compos[y][x];
                if (c != null) {
                    pg.addComponent(c);
                }
            }
            hg.addGroup(pg);
        }
        layout.setHorizontalGroup(hg);

        SequentialGroup vg = layout.createSequentialGroup();
        for (int y = 0; y < ny; y++) {
            ParallelGroup pg = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
            for (int x = 0; x < nx; x++) {
                JComponent c = compos[y][x];
                if (c != null) {
                    pg.addComponent(c);
                }
            }
            vg.addGroup(pg);
        }
        layout.setVerticalGroup(vg);

        return panel;
    }

    private Border createTitledBorder(String title, boolean last) {
        return new CompoundBorder(new EmptyBorder(10, 10, last ? 10 : 0, 10), new CompoundBorder(
            new TitledBorder(title),
            new EmptyBorder(0, 10, 5, 10)));
    }

    private JEditorPane createLink(String url) {
        return createTextAndLink("", url, 100);
    }

    private JEditorPane createTextAndLink(String appendText, String url, int width) {
        String str = String.format("%1$s<a href=\"%2$s\">%2$s</a>", appendText, url);
        JEditorPane text = new JEditorPane("text/html", str);

        text.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        text.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        text.setEditable(false);
        text.setOpaque(false);
        text.setMaximumSize(new Dimension(width, 20));

        text.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == EventType.ACTIVATED) {
                    URL url = e.getURL();
                    Desktop dp = Desktop.getDesktop();
                    try {
                        dp.browse(url.toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        return text;
    }

    private boolean isValid(JComponent[][] compos) {
        for (JComponent[] c : compos) {
            if (((JTextField) c[1]).getText().length() == 0) {
                JOptionPane.showMessageDialog(MainFrame.this, "Please input " + ((JLabel) c[0]).getText() + ".");
                return false;
            }
        }
        return true;
    }

    private String getDiagramId(JTextField tf) {
        String text = tf.getText();
        int index = text.lastIndexOf("/");
        return text.substring(index + 1);
    }

    private String toDiagramUrl(String key) {
        return String.format("%s/diagrams/%s", ROOT_PATH, key);
    }
}
