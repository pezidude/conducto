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

    public static Document filterParts(Document originalDoc, List<String> selectedPartIds) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDoc = builder.newDocument();

        if (originalDoc.getDocumentElement() != null) {
            Node importedRoot = newDoc.importNode(originalDoc.getDocumentElement(), true);
            newDoc.appendChild(importedRoot);
        }

        Set<String> selectedSet = new HashSet<>(selectedPartIds);

        // Remove parts that are not selected from part-list
        NodeList partListNodes = newDoc.getElementsByTagName("score-part");
        List<Node> partsToRemoveFromList = new ArrayList<>();
        for (int i = 0; i < partListNodes.getLength(); i++) {
            Element part = (Element) partListNodes.item(i);
            String id = part.getAttribute("id");
            if (!selectedSet.contains(id)) {
                partsToRemoveFromList.add(part);
            }
        }
        for (Node n : partsToRemoveFromList) {
            if (n.getParentNode() != null) {
                n.getParentNode().removeChild(n);
            }
        }

        // Remove actual part elements
        NodeList partNodes = newDoc.getElementsByTagName("part");
        List<Node> partsToRemove = new ArrayList<>();
        for (int i = 0; i < partNodes.getLength(); i++) {
            Element part = (Element) partNodes.item(i);
            String id = part.getAttribute("id");
            if (!selectedSet.contains(id)) {
                partsToRemove.add(part);
            }
        }
        for (Node n : partsToRemove) {
            if (n.getParentNode() != null) {
                n.getParentNode().removeChild(n);
            }
        }

        return newDoc;
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
        Set<String> usedStaffs = new HashSet<>();
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
                    } else {
                        // Track used staffs
                        NodeList staffNodes = note.getElementsByTagName("staff");
                        if (staffNodes.getLength() > 0) {
                            usedStaffs.add(staffNodes.item(0).getTextContent());
                        }
                    }
                }
            }
            for (Node n : toRemove) {
                measure.removeChild(n);
            }
        }

        // Cleanup empty staffs if possible
        if (!usedStaffs.isEmpty()) {
            cleanupStaffs(part, usedStaffs);
        }
    }

    private static void cleanupStaffs(Element part, Set<String> usedStaffs) {
        // If all staffs are used, nothing to do
        // (This is a simplified check, ideally we'd check the 'staves' attribute)
        
        NodeList stavesNodes = part.getElementsByTagName("staves");
        if (stavesNodes.getLength() > 0) {
            int originalStaves = 1;
            try {
                originalStaves = Integer.parseInt(stavesNodes.item(0).getTextContent());
            } catch (Exception e) {}

            if (usedStaffs.size() < originalStaves) {
                // Update staves count
                for (int i = 0; i < stavesNodes.getLength(); i++) {
                    stavesNodes.item(i).setTextContent(String.valueOf(usedStaffs.size()));
                }

                // If only one staff remains, we can often just remove the <staff> element from notes
                // and remove extra <clef> elements.
                if (usedStaffs.size() == 1) {
                    String remainingStaff = usedStaffs.iterator().next();
                    removeStaffNumbering(part, remainingStaff);
                }
            }
        }
    }

    private static void removeStaffNumbering(Element part, String remainingStaff) {
        // Remove <staff> element from notes
        NodeList notes = part.getElementsByTagName("note");
        for (int i = 0; i < notes.getLength(); i++) {
            Element note = (Element) notes.item(i);
            NodeList staffNodes = note.getElementsByTagName("staff");
            for (int j = 0; j < staffNodes.getLength(); j++) {
                note.removeChild(staffNodes.item(j));
            }
        }

        // Keep only the clef for the remaining staff
        NodeList clefs = part.getElementsByTagName("clef");
        List<Node> clefsToRemove = new ArrayList<>();
        for (int i = 0; i < clefs.getLength(); i++) {
            Element clef = (Element) clefs.item(i);
            String number = clef.getAttribute("number");
            if (number != null && !number.isEmpty() && !number.equals(remainingStaff)) {
                clefsToRemove.add(clef);
            }
        }
        for (Node n : clefsToRemove) {
            n.getParentNode().removeChild(n);
        }
        
        // Remove number attribute from remaining clef if it's now the only one
        NodeList remainingClefs = part.getElementsByTagName("clef");
        if (remainingClefs.getLength() == 1) {
            ((Element)remainingClefs.item(0)).removeAttribute("number");
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