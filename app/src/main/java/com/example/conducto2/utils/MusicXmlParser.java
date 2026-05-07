package com.example.conducto2.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MusicXmlParser {

    public static class PartInfo {
        public String id;
        public String name;
        public Set<String> voices = new HashSet<>();

        public PartInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static Map<String, PartInfo> getPartsAndVoices(Document doc) {
        Map<String, PartInfo> partMap = new HashMap<>();

        // Extract part names from part-list
        NodeList partList = doc.getElementsByTagName("score-part");
        for (int i = 0; i < partList.getLength(); i++) {
            Element part = (Element) partList.item(i);
            String id = part.getAttribute("id");
            String name = "";
            NodeList nameNodes = part.getElementsByTagName("part-name");
            if (nameNodes.getLength() > 0) {
                name = nameNodes.item(0).getTextContent();
            }
            partMap.put(id, new PartInfo(id, name));
        }

        // Extract voices for each part
        NodeList parts = doc.getElementsByTagName("part");
        for (int i = 0; i < parts.getLength(); i++) {
            Element part = (Element) parts.item(i);
            String id = part.getAttribute("id");
            PartInfo info = partMap.get(id);
            if (info == null) continue;

            NodeList notes = part.getElementsByTagName("note");
            for (int j = 0; j < notes.getLength(); j++) {
                Element note = (Element) notes.item(j);
                NodeList voiceNodes = note.getElementsByTagName("voice");
                if (voiceNodes.getLength() > 0) {
                    info.voices.add(voiceNodes.item(0).getTextContent());
                } else {
                    // If no voice is specified, it's implicitly voice 1 (or depends on context, but let's say "1")
                    info.voices.add("1");
                }
            }
        }

        return partMap;
    }

    public static Document filterVoices(Document originalDoc, Map<String, Set<String>> selectedVoicesPerPart) throws Exception {
        // On Android, cloning the Document node (type 9) is often not supported.
        // Instead, we create a new document and import the document element.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDoc = builder.newDocument();

        if (originalDoc.getDocumentElement() != null) {
            Node importedRoot = newDoc.importNode(originalDoc.getDocumentElement(), true);
            newDoc.appendChild(importedRoot);
        }

        // Remove parts that are not selected at all
        NodeList partListNodes = newDoc.getElementsByTagName("score-part");
        List<Node> partsToRemoveFromList = new ArrayList<>();
        for (int i = 0; i < partListNodes.getLength(); i++) {
            Element part = (Element) partListNodes.item(i);
            String id = part.getAttribute("id");
            if (!selectedVoicesPerPart.containsKey(id) || selectedVoicesPerPart.get(id).isEmpty()) {
                partsToRemoveFromList.add(part);
            }
        }
        for (Node n : partsToRemoveFromList) {
            n.getParentNode().removeChild(n);
        }

        NodeList partNodes = newDoc.getElementsByTagName("part");
        List<Node> partsToRemove = new ArrayList<>();
        for (int i = 0; i < partNodes.getLength(); i++) {
            Element part = (Element) partNodes.item(i);
            String id = part.getAttribute("id");
            if (!selectedVoicesPerPart.containsKey(id) || selectedVoicesPerPart.get(id).isEmpty()) {
                partsToRemove.add(part);
            } else {
                // Filter voices within this part
                Set<String> selectedVoices = selectedVoicesPerPart.get(id);
                filterVoicesInPart(part, selectedVoices);
            }
        }
        for (Node n : partsToRemove) {
            n.getParentNode().removeChild(n);
        }

        return newDoc;
    }

    private static void filterVoicesInPart(Element part, Set<String> selectedVoices) {
        NodeList measures = part.getElementsByTagName("measure");
        for (int i = 0; i < measures.getLength(); i++) {
            Element measure = (Element) measures.item(i);
            NodeList children = measure.getChildNodes();
            List<Node> toRemove = new ArrayList<>();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeName().equals("note")) {
                    Element note = (Element) child;
                    NodeList voiceNodes = note.getElementsByTagName("voice");
                    String voice = "1";
                    if (voiceNodes.getLength() > 0) {
                        voice = voiceNodes.item(0).getTextContent();
                    }
                    if (!selectedVoices.contains(voice)) {
                        toRemove.add(note);
                    }
                }
                // Optional: handle backup/forward? 
                // For now, let's keep them and see if OSMD handles "empty" measure segments.
                // If we remove all notes of a voice, the backups/forwards associated with it might cause issues.
                // But MusicXML is quite forgiving if notes are missing.
            }
            for (Node n : toRemove) {
                measure.removeChild(n);
            }
        }
    }

    public static String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}