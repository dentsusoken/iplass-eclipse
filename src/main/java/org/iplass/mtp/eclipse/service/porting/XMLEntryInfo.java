/*
 * Copyright 2018 DENTSU SOKEN INC. All Rights Reserved.
 */
package org.iplass.mtp.eclipse.service.porting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iplass.mtp.impl.metadata.MetaDataEntry;

public class XMLEntryInfo implements Serializable {
	private static final long serialVersionUID = 1499106213312696435L;

	private Map<String, MetaDataEntry> pathEntryMap = new HashMap<>();

	private Map<String, MetaDataEntry> idEntryMap = new HashMap<>();

	private List<MetaDataEntry> idBlankEntryList = new ArrayList<>();

	public XMLEntryInfo() {
	}

	public Map<String, MetaDataEntry> getPathEntryMap() {
		return pathEntryMap;
	}

	public void setPathEntryMap(Map<String, MetaDataEntry> pathEntryMap) {
		this.pathEntryMap = pathEntryMap;
	}

	public void putPathEntry(String path, MetaDataEntry entry) {
		pathEntryMap.put(path, entry);
	}

	public boolean containsPathEntry(String path) {
		return pathEntryMap.containsKey(path);
	}

	public MetaDataEntry getPathEntry(String path) {
		return pathEntryMap.get(path);
	}

	public Map<String, MetaDataEntry> getIdEntryMap() {
		return idEntryMap;
	}

	public void setIdEntryMap(Map<String, MetaDataEntry> idEntryMap) {
		this.idEntryMap = idEntryMap;
	}

	public void putIdEntry(String id, MetaDataEntry entry) {
		idEntryMap.put(id, entry);
	}

	public boolean containsIdEntry(String id) {
		return idEntryMap.containsKey(id);
	}

	public MetaDataEntry getIdEntry(String id) {
		return idEntryMap.get(id);
	}

	public List<MetaDataEntry> getIdBlankEntryList() {
		return idBlankEntryList;
	}

	public void setIdBlankEntryList(List<MetaDataEntry> idBlankEntryList) {
		this.idBlankEntryList = idBlankEntryList;
	}

	public void addIdBlankEntry(MetaDataEntry idBlankEntry) {
		idBlankEntryList.add(idBlankEntry);
	}
}
