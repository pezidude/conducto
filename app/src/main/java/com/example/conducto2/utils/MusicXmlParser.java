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

/**
 * MusicXmlParser
 * 
 * A specialized utility engine for the DOM-based manipulation of MusicXML scores.
 * Unlike {@link FileHelper} which uses stream parsing for read-only validation, 
 * this class loads the entire XML tree into memory to allow for complex structural mutations.
 * 
 * Its primary role is to support the Role Grouping feature by extracting instrument metadata 
 * and surgically stripping out unselected parts from a master score to generate 
 * role-specific sheet music files.
 */
public class MusicXmlParser {

    /**
     * Data structure holding extracted metadata for a specific instrument part.
     */
    public static class PartInfo {
        /** The internal XML identifier (e.g., "P1"). */
        public String id;
        /** The human-readable name of the instrument (e.g., "Violin"). */
        public String name;
        /** A set of distinct musical voices found within this part. */
        public Set<String> voices = new HashSet<>();

        public PartInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * Scans a MusicXML document to build a comprehensive map of all available instruments.
     * 
     * @param doc The parsed DOM Document of the master score.
     * @return A map linking part IDs (e.g., "P1") to their extracted PartInfo metadata.
     */
    public static Map<String, PartInfo> getPartsAndVoices(Document doc) {
        Map<String, PartInfo> partMap = new HashMap<>();

        // Phase 1: Header Scanning
        // Extract part names and IDs from the <part-list> header definition.
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

        // Phase 2: Content Scanning
        // Iterate through the actual musical content (<part> nodes) to identify distinct voices.
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
                    // Implicit context: If no voice tag is present, it defaults to voice 1.
                    info.voices.add("1");
                }
            }
        }

        return partMap;
    }

    /**
     * Performs a structural mutation on the DOM tree to isolate specific instrumental parts.
     * Creates a deep clone of the original document and strips away unselected nodes.
     * 
     * @param originalDoc The master score DOM.
     * @param selectedPartIds The list of IDs (e.g., ["P1", "P3"]) to retain.
     * @return A new Document containing only the selected parts.
     * @throws Exception If XML building or cloning fails.
     */
    public static Document filterParts(Document originalDoc, List<String> selectedPartIds) throws Exception {
        // Step 1: Initialize a new empty DOM structure.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDoc = builder.newDocument();

        // Step 2: Deep Clone the original document into the new structure.
        // Android's DOM implementation often restricts direct node cloning, so importing is required.
        if (originalDoc.getDocumentElement() != null) {
            Node importedRoot = newDoc.importNode(originalDoc.getDocumentElement(), true);
            newDoc.appendChild(importedRoot);
        }

        Set<String> selectedSet = new HashSet<>(selectedPartIds);

        // Step 3: Clean the Header
        // Remove unselected definitions from the <part-list> element.
        NodeList partListNodes = newDoc.getElementsByTagName("score-part");
        List<Node> partsToRemoveFromList = new ArrayList<>();
        for (int i = 0; i < partListNodes.getLength(); i++) {
            Element part = (Element) partListNodes.item(i);
            String id = part.getAttribute("id");
            if (!selectedSet.contains(id)) {
                partsToRemoveFromList.add(part); // Stage for deletion to avoid concurrent modification errors
            }
        }
        for (Node n : partsToRemoveFromList) {
            if (n.getParentNode() != null) {
                n.getParentNode().removeChild(n);
            }
        }

        // Step 4: Clean the Content
        // Remove the actual <part> containers carrying the musical notes for unselected instruments.
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

    /**
     * Granular mutation: Filters specific voices within selected parts.
     * Operates similarly to filterParts but drills down into individual <measure> and <note> elements.
     */
    public static Document filterVoices(Document originalDoc, Map<String, Set<String>> selectedVoicesPerPart) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDoc = builder.newDocument();

        if (originalDoc.getDocumentElement() != null) {
            Node importedRoot = newDoc.importNode(originalDoc.getDocumentElement(), true);
            newDoc.appendChild(importedRoot);
        }

        // Remove parts that are completely unselected.
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
                // Drill down: Filter specific voices within this retained part.
                Set<String> selectedVoices = selectedVoicesPerPart.get(id);
                filterVoicesInPart(part, selectedVoices);
            }
        }
        for (Node n : partsToRemove) {
            n.getParentNode().removeChild(n);
        }

        return newDoc;
    }

    /**
     * Helper logic for filterVoices that iterates through measures and removes individual <note> nodes.
     */
    private static void filterVoicesInPart(Element part, Set<String> selectedVoices) {
        Set<String> usedStaffs = new HashSet<>();
        NodeList measures = part.getElementsByTagName("measure");
        for (int i = 0; i < measures.getLength(); i++) {
            Element measure = (Element) measures.item(i);
            NodeList children = measure.getChildNodes();
            List<Node> toRemove = new ArrayList<>();
            
            // Analyze each element inside the measure
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeName().equals("note")) {
                    Element note = (Element) child;
                    NodeList voiceNodes = note.getElementsByTagName("voice");
                    String voice = "1";
                    if (voiceNodes.getLength() > 0) {
                        voice = voiceNodes.item(0).getTextContent();
                    }
                    
                    // Stage note for deletion if its voice isn't selected
                    if (!selectedVoices.contains(voice)) {
                        toRemove.add(note);
                    } else {
                        // Track used staffs for potential cleanup later
                        NodeList staffNodes = note.getElementsByTagName("staff");
                        if (staffNodes.getLength() > 0) {
                            usedStaffs.add(staffNodes.item(0).getTextContent());
                        }
                    }
                }
            }
            // Execute removals
            for (Node n : toRemove) {
                measure.removeChild(n);
            }
        }

        // Cleanup empty staffs to prevent rendering artifacts
        if (!usedStaffs.isEmpty()) {
            cleanupStaffs(part, usedStaffs);
        }
    }

    /**
     * Helper logic to correct staff numbering metadata if voices were removed.
     */
    private static void cleanupStaffs(Element part, Set<String> usedStaffs) {
        NodeList stavesNodes = part.getElementsByTagName("staves");
        if (stavesNodes.getLength() > 0) {
            int originalStaves = 1;
            try {
                originalStaves = Integer.parseInt(stavesNodes.item(0).getTextContent());
            } catch (Exception e) {}

            if (usedStaffs.size() < originalStaves) {
                // Update the global staves count declaration
                for (int i = 0; i < stavesNodes.getLength(); i++) {
                    stavesNodes.item(i).setTextContent(String.valueOf(usedStaffs.size()));
                }

                // If only one staff remains, remove specific staff numbering from notes
                // and prune redundant <clef> declarations.
                if (usedStaffs.size() == 1) {
                    String remainingStaff = usedStaffs.iterator().next();
                    removeStaffNumbering(part, remainingStaff);
                }
            }
        }
    }

    private static void removeStaffNumbering(Element part, String remainingStaff) {
        // Remove <staff> elements from notes
        NodeList notes = part.getElementsByTagName("note");
        for (int i = 0; i < notes.getLength(); i++) {
            Element note = (Element) notes.item(i);
            NodeList staffNodes = note.getElementsByTagName("staff");
            for (int j = 0; j < staffNodes.getLength(); j++) {
                note.removeChild(staffNodes.item(j));
            }
        }

        // Prune redundant clefs
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
        
        // Final cleanup: remove 'number' attribute from the last remaining clef
        NodeList remainingClefs = part.getElementsByTagName("clef");
        if (remainingClefs.getLength() == 1) {
            ((Element)remainingClefs.item(0)).removeAttribute("number");
        }
    }

    /**
     * Serializes a modified DOM Document back into a formatted XML String.
     * Uses the standard javax.xml Transformer API.
     * 
     * @param doc The DOM to serialize.
     * @return Raw string content ready for cloud storage upload.
     */
    public static String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}