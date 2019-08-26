package org.cytoscape.app.communitydetection.edgelist.writer;

import java.io.IOException;
import java.io.OutputStream;

import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriterTask implements CyWriter {

	private final static Logger logger = LoggerFactory.getLogger(WriterTask.class);

	private final OutputStream outStream;
	private final CyNetwork network;
	private final String attribute;

	public WriterTask(OutputStream outStream, CyNetwork network, String attribute) {
		this.outStream = outStream;
		this.network = network;
		this.attribute = attribute;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		if (attribute.equals("none")) {
			for (CyEdge edge : network.getEdgeList()) {
				String s = edge.getSource().getSUID().toString() + "\t" + edge.getTarget().getSUID().toString() + "\n";
				outStream.write(s.getBytes());
			}
		} else {
			if (!CyTableUtil.getColumnNames(network.getDefaultEdgeTable()).contains(attribute)) {
				System.out.println(attribute);
				throw new Exception(attribute + " is not a valid column in edge table.");
			}
			for (CyEdge edge : network.getEdgeList()) {
				String s = edge.getSource().getSUID().toString() + "\t" + edge.getTarget().getSUID().toString() + "\t"
						+ network.getRow(edge).get(attribute, getColumnType()) + "\n";
				outStream.write(s.getBytes());
			}
		}
	}

	@Override
	public void cancel() {
		if (outStream == null) {
			return;
		}
		try {
			outStream.close();
		} catch (IOException e) {
			logger.error("Could not close Outputstream for EdgeNetworkWriter.", e);
		}
	}

	private Class<?> getColumnType() {
		return network.getDefaultEdgeTable().getColumn(attribute).getType();
	}
}
