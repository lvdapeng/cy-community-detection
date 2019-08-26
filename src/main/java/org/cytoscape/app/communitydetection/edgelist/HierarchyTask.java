package org.cytoscape.app.communitydetection.edgelist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.cytoscape.app.communitydetection.edgelist.reader.ReaderTask;
import org.cytoscape.app.communitydetection.edgelist.reader.ReaderTaskFactoryImpl;
import org.cytoscape.app.communitydetection.edgelist.writer.WriterTaskFactoryImpl;
import org.cytoscape.app.communitydetection.rest.CDRestClient_Java;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;

public class HierarchyTask extends AbstractTask {

	private final CyNetwork network;
	private final String algorithm;
	private final String attribute;

	public HierarchyTask(CyNetwork network, String algorithm, String attribute) {
		this.network = network;
		this.algorithm = algorithm;
		this.attribute = attribute;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Community Detection: Creating Hierarchy Network");
		taskMonitor.setStatusMessage("Exporting the network");
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		WriterTaskFactoryImpl writerFactory = (WriterTaskFactoryImpl) TaskListenerFactory.getInstance()
				.getEdgeListWriterFactory();
		CyWriter writer = writerFactory.createWriter(outStream, network, attribute);
		writer.run(taskMonitor);
		String resultURI = CDRestClient_Java.getInstance().postEdgeList(algorithm, false, outStream.toString());
		taskMonitor.setProgress(0.1);
		if (cancelled) {
			return;
		}
		taskMonitor.setStatusMessage("Network exported, retrieving the hierarchy");
		CommunityDetectionResult cdResult = CDRestClient_Java.getInstance().getEdgeList(resultURI, taskMonitor);
		if (cancelled) {
			return;
		}
		InputStream inStream = new ByteArrayInputStream(cdResult.getResult().trim().replace(';', '\n').getBytes());
		taskMonitor.setProgress(0.9);
		taskMonitor.setStatusMessage("Received heirarchy, creating a new network");
		ReaderTaskFactoryImpl readerFactory = (ReaderTaskFactoryImpl) TaskListenerFactory.getInstance()
				.getEdgeListReaderFactory();
		TaskIterator iterator = readerFactory.createTaskIterator(inStream, null, network.getSUID());
		ReaderTask reader = (ReaderTask) iterator.next();
		reader.run(taskMonitor);
		reader.setNetworkName(algorithm, attribute);
		taskMonitor.setProgress(0.95);

		taskMonitor.setStatusMessage("Creating a view for the network");
		reader.buildCyNetworkView(reader.getNetworks()[0]);
		taskMonitor.setProgress(1.0);
	}

	@Override
	public void cancel() {
		CDRestClient_Java.getInstance().setTaskCanceled(true);
		super.cancel();
	}

}
