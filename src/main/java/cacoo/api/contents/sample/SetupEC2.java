package cacoo.api.contents.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;

public class SetupEC2 {
	private static final String EC2_STENCIL_ID = "66456";
	private static final String EBS_STENCIL_ID = "B500C";

	private String apiKey;
	private String diagramId;
	private String accessKey;
	private String secretKey;
	private String endpoint;
	private String keyName;
	private String zone;
	private String securityGroup;

	public SetupEC2 withApiKey(String apiKey) {
		this.apiKey = apiKey;
		return this;
	}

	public SetupEC2 withDiagramId(String diagramId) {
		this.diagramId = diagramId;
		return this;
	}

	public SetupEC2 withAccessKey(String accessKey) {
		this.accessKey = accessKey;
		return this;
	}

	public SetupEC2 withSecretKey(String secretKey) {
		this.secretKey = secretKey;
		return this;
	}

	public SetupEC2 withEndpoint(String endpoint) {
		this.endpoint = endpoint;
		return this;
	}

	public SetupEC2 withKeyName(String keyName) {
		this.keyName = keyName;
		return this;
	}

	public SetupEC2 withZone(String zone) {
		this.zone = zone;
		return this;
	}

	public SetupEC2 withSecurityGroup(String securityGroup) {
		this.securityGroup = securityGroup;
		return this;
	}

	public void execute() throws Exception {
		String url = String.format("%s/api/v1/diagrams/%s/contents.xml?returnValues=uid&apiKey=%s", MainFrame.ROOT_PATH, diagramId, apiKey);
		ContentsParser parser = new ContentsParser();
		parser.parse(url);

		EC2Creater creater = new EC2Creater();
		creater.create(parser.instances, parser.volumes);
	}

	private class ContentsParser {
		XPath xpath;
		Document document;
		List<EC2Instance> instances = new ArrayList<EC2Instance>();
		List<EBSVolume> volumes = new ArrayList<EBSVolume>();
		Map<String, EC2Instance> uidInstanceMap = new HashMap<String, EC2Instance>();

		ContentsParser() {
			xpath = XPathFactory.newInstance().newXPath();
		}

		void parse(String url) throws Exception {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			System.out.printf("start parse xml (url: %s)\n", url);

			document = builder.parse(url);
			parseEC2();
			parseEBS();
		}

		void parseEC2() throws Exception {
			NodeList instanceNodes = (NodeList) xpath.evaluate(String.format("//group[@attr-stencil-id='%s']", EC2_STENCIL_ID),
					document, XPathConstants.NODESET);

			System.out.println();
			System.out.println("EC2 instances");
			for (int i = 0; i < instanceNodes.getLength(); i++) {
				Node node = instanceNodes.item(i);
				String name = getTextValue("name", node);
				String ami = getTextValue("ami", node);
				String type = getTextValue("type", node);

				EC2Instance instance = new EC2Instance(name, ami, type);
				instances.add(instance);
				uidInstanceMap.put(((Element) node).getAttribute("uid"), instance);
				System.out.printf(" - %s\n", instance);
			}
		}

		void parseEBS() throws Exception {
			NodeList ebsNodes = (NodeList) xpath.evaluate(String.format("//group[@attr-stencil-id='%s']", EBS_STENCIL_ID),
					document, XPathConstants.NODESET);

			System.out.println();
			System.out.println("EBS Volumes");
			for (int i = 0; i < ebsNodes.getLength(); i++) {
				Node node = ebsNodes.item(i);
				String name = getTextValue("name", node);
				String capacity = getTextValue("capacity", node);
				String device = getTextValue("device", node);
				String snapshot = getTextValue("snapshot", node);

				EBSVolume volume = new EBSVolume(name, capacity, device, snapshot);
				volumes.add(volume);
				System.out.printf(" - %s", volume);
				EC2Instance instance = getInstance(node);
				if (instance != null) {
					instance.volumes.add(volume);
					System.out.printf(", instance: %s", instance.name);
				}
				System.out.println();
			}
		}

		private String getTextValue(String attrName, Node pNode) throws XPathExpressionException {
			return xpath.evaluate("group[@attr-name='" + attrName + "']/text/text()", pNode);
		}

		EC2Instance getInstance(Node node) throws Exception {
			String uid = ((Element) node).getAttribute("uid");
			String expression = String.format("//line[start/@connect-uid='%1$s']/end/@connect-uid | //line[end/@connect-uid='%1$s']/start/@connect-uid", uid);
			   
			NodeList uidNodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
			int len = uidNodes.getLength();
			for (int j = 0; j < len; j++) {
				String instanceUid = ((Attr) uidNodes.item(j)).getValue();
				if (uidInstanceMap.containsKey(instanceUid)) {
					return uidInstanceMap.get(instanceUid);
				}
			}
			return null;
		}
	}

	private class EC2Creater {
		AmazonEC2 ec2;
		Placement zone;

		EC2Creater() throws Exception {
			ec2 = new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));
			ec2.setEndpoint(endpoint);

			this.zone = new Placement(SetupEC2.this.zone);
		}

		void create(List<EC2Instance> instances, List<EBSVolume> volumes) throws Exception {
			createEC2(instances);
			createEBS(volumes);
			attachVolumes(instances);
		}

		void createEC2(List<EC2Instance> instances) {
			System.out.println();
			for (EC2Instance instance : instances) {
				RunInstancesRequest request = new RunInstancesRequest(instance.ami, 1, 1).withInstanceType(
						instance.type).withKeyName(keyName).withPlacement(zone).withSecurityGroups(securityGroup);
				RunInstancesResult result = ec2.runInstances(request);
				instance.instance = result.getReservation().getInstances().get(0);
				String instanceId = instance.instance.getInstanceId();
				createNameTag(instanceId, instance.name);

				System.out.printf("create instance instance-id: %s, name: %s\n", instanceId, instance.name);
			}
		}

		void createEBS(List<EBSVolume> volumes) {
			System.out.println();
			for (EBSVolume volume : volumes) {
				CreateVolumeRequest request = new CreateVolumeRequest(volume.size, SetupEC2.this.zone).withSize(1);
				if (StringUtils.isNotBlank(volume.snapshot)) {
					request.setSnapshotId(volume.snapshot);
				}
				volume.volume = ec2.createVolume(request).getVolume();
				String volumeId = volume.volume.getVolumeId();
				createNameTag(volumeId, volume.name);
				System.out.printf("create volume volume-id: %s, name: %s\n", volumeId, volume.name);
			}
		}

		void createNameTag(String id, String name) {
			ec2.createTags(new CreateTagsRequest().withResources(id).withTags(new Tag("Name", name)));
		}

		void attachVolumes(List<EC2Instance> instances) throws Exception {
			HashMap<String, EC2Instance> instanceIdMap = new HashMap<String, EC2Instance>();
			for (EC2Instance instance : instances) {
				if (instance.volumes.size() > 0) {
					instanceIdMap.put(instance.instance.getInstanceId(), instance);
				}
			}

			if (instanceIdMap.size() == 0) {
				return;
			}

			System.out.println();
			// check every 3 seconds
			for (int i = 0; i < 50; i++) {
				System.out.println("check running instance");
				DescribeInstanceStatusRequest request = new DescribeInstanceStatusRequest().withInstanceIds(
						instanceIdMap.keySet()).withFilters(
						new Filter("instance-state-name", Collections.singletonList("running")));
				DescribeInstanceStatusResult rest = ec2.describeInstanceStatus(request);

				for (InstanceStatus st : rest.getInstanceStatuses()) {
					String id = st.getInstanceId();
					attachVolumes(instanceIdMap.get(id));
					instanceIdMap.remove(id);
				}

				if (instanceIdMap.isEmpty()) {
					break;
				}

				Thread.sleep(3000);
			}
		}

		void attachVolumes(EC2Instance instance) {
			String instanceId = instance.instance.getInstanceId();
			System.out.printf("\nattach volumes (instance-id: %s, name: %s)\n", instanceId, instance.name);
			for (EBSVolume volume : instance.volumes) {
				String volumeId = volume.volume.getVolumeId();
				ec2.attachVolume(new AttachVolumeRequest(volumeId, instanceId, volume.device));
				System.out.printf(" - volume-id: %s, name:%s, device: %s\n", volumeId, volume.name, volume.device);
			}
		}
	}

	private static class EC2Instance {
		String name;
		String ami;
		InstanceType type;
		Instance instance;
		List<EBSVolume> volumes = new ArrayList<EBSVolume>();

		EC2Instance(String name, String ami, String type) {
			this.name = name;
			this.ami = ami;
			this.type = InstanceType.fromValue(type);
		}

		public String toString() {
			return String.format("name: %s, ami: %s, type: %s", name, ami, type);
		}
	}

	private static class EBSVolume {
		String name;
		int size;
		String device;
		String snapshot;
		Volume volume;

		EBSVolume(String name, String size, String device, String snapshot) {
			this.name = name;
			this.size = parseSize(size);
			this.device = device;
			this.snapshot = snapshot;
		}

		public String toString() {
			return String.format("name: %s, size: %dGB, device: %s, snapshot: %s", name, size, device, snapshot);
		}

		static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)\\s*(?:([GT])B?)?");

		static int parseSize(String size) {
			Matcher m = SIZE_PATTERN.matcher(size);
			boolean matches = m.matches();
			if (!matches) {
				throw new RuntimeException("size parse error - " + size);
			}

			int value = Integer.parseInt(m.group(1));
			String unit = m.group(2);
			if ("T".equals(unit)) {
				return value * 1024;
			} else {
				return value;
			}
		}
	}

}
