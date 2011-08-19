package org.opendatakit.briefcase.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.IFormDefinition;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.util.ServerFormListFetcher.FormListException;

public class RetrieveAvailableFormsFromServer {
	ServerConnectionInfo originServerInfo;
	List<FormStatus> formStatuses = new ArrayList<FormStatus>();
	
	public RetrieveAvailableFormsFromServer(
			ServerConnectionInfo originServerInfo) {
		this.originServerInfo = originServerInfo;
	}

	public void doAction() throws IOException, FormListException {
		Document doc = Aggregate10Utils.retrieveAvailableFormsFromServer(originServerInfo);
		ServerFormListFetcher fetcher =	new ServerFormListFetcher(originServerInfo);
		List<RemoteFormDefinition> formDefs = fetcher.parseFormListResponse(doc);
		for ( IFormDefinition fd : formDefs ) {
			formStatuses.add(new FormStatus(fd));
		}
	}

	public List<FormStatus> getAvailableForms() {
		return formStatuses;
	}

}
