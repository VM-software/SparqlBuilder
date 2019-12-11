/*
 * 
 * GNU General Public License v3.0
 * 
 */

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Robot;
import java.awt.AWTException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Mit der Klasse SparqlBuilder können Sie SPARQL-Abfragen erstellen
 * 
 */

public class SparqlBuilder extends JPanel {
    
    JLabel resultLabel;
    JButton generate_query, info_button, count_request;
    Thread countingThread;
    JList<String> filterNotExistList, filterList;
    DefaultListModel<String> filterNotExistModel, filterModel;
    SparqlArea area51;
    JFrame parentFrame;
    String endPoint;
    boolean frameCreated = true;
    int timeInMillisec = 5000;
    String infoLabel = "Information about using this Tool:\n"
            + "In the first area you can enter rdf statement, in which each field has a help menu\n"
            + "In the second area, you have the option to enter filter not exists statement\n"
            + "In the third area you can enter filter expressions";
    
    /**
     * Klasse SparqlArea ermöglicht die Eingabe von RDF-Anweisungen
     */
    public class SparqlArea extends JPanel{

        Box vertLineBox, globVertBox, buttonBox; 
        JButton addNewLine;
        List<Line> lineList;
        int lineLen;
        
        /**
         * Klasse Line stellt einen RDF-Anweisung dar
         */
        public class Line {
            
            JTextField subject, predicate, object;
            JPopupMenu searchPopUp;
            JButton delLineButton;
            JCheckBox optCheck;
            Box lineBox;
            ArrayList<JTextField> currentLine;
            Thread searchThread;
            Thread searchTimer;
                      
            Line(final Box listOfTriples){
                
                currentLine = new ArrayList();
                delLineButton = new JButton("-"); optCheck = new JCheckBox();
                subject = new JTextField("?subject", lineLen);
                predicate = new JTextField("?predicate", lineLen);
                object = new JTextField("?object", lineLen);
                searchPopUp = new JPopupMenu();
                subject.add(searchPopUp);
                subject.setComponentPopupMenu(searchPopUp);
                predicate.add(searchPopUp);
                predicate.setComponentPopupMenu(searchPopUp);
                object.add(searchPopUp);
                object.setComponentPopupMenu(searchPopUp);
                lineBox = Box.createHorizontalBox(); lineBox.add(optCheck); 
                lineBox.add(subject);lineBox.add(predicate);lineBox.add(object);lineBox.add(delLineButton);
                currentLine.add(subject);currentLine.add(predicate);currentLine.add(object);
                subject.getDocument().addDocumentListener(getDocListnerToField(subject, "?aimVar__ ?b__ ?c__ \n}LIMIT 1000"));
                predicate.getDocument().addDocumentListener(getDocListnerToField(predicate, "?aimVar__ ?c__ \n}LIMIT 41"));
                object.getDocument().addDocumentListener(getDocListnerToField(object, "?aimVar__ \n}LIMIT 1000"));
                subject.addKeyListener(setPopUpFocused(true));
                predicate.addKeyListener(setPopUpFocused(true));
                object.addKeyListener(setPopUpFocused(true));
                searchPopUp.removeAll();
                searchPopUp.addKeyListener(setPopUpFocused(false));
                delLineButton.addActionListener(new ActionListener(){ 
                    public void actionPerformed(ActionEvent e) {
                        lineList.remove(SparqlArea.Line.this);
                        listOfTriples.remove(lineBox);
                        getParentFrame().setVisible(true);
                    } 
                });                
                
            }
            
            /**
             * Fängt ein Ereignis, wenn Benutzer die Down-Taste drücken(der Fokus auf das Menü gelegt wird)
             * @param isTextField
             * @return KeyListener
             */
            public KeyListener setPopUpFocused(final boolean isTextField){
                return new KeyListener(){
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if(e.getKeyCode() == KeyEvent.VK_DOWN){
                            searchPopUp.setVisible(false);
                            searchPopUp.setVisible(true);
                            if(isTextField){ 
                                searchPopUp.requestFocus();
                                try{
                                    Robot iRobot = new Robot();
                                    iRobot.keyPress(KeyEvent.VK_DOWN);
                                }catch (AWTException exc) {
                                        exc.printStackTrace();
                                }
                            }
                        }
                    }
                    @Override
                    public void keyReleased(KeyEvent e){}
                    @Override
                    public void keyTyped(KeyEvent e){}
                };
            }
            
            /**
             * Gibt  subjekt prädikate objekt als Liste zurück
             * @return ArrayList
             */
            public ArrayList<JTextField> getCurrentLine(){
                return currentLine;
            }
            
            /**
             * Wenn Benutzer mit einem den Widget interagiert, wird ein Hilfemenü angezeigt
             * @param fieldObj Aktuelle Widget, mit dem der Benutzer interagiert
             * @param aimStr rest des SPARQL-Abfrage
             * @return DocumentListener
             */
            public DocumentListener getDocListnerToField(final JTextField fieldObj, final String aimStr){
                return new DocumentListener() {
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        if(getFrameCreated()) setValueChanged();
                    }
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        if(getFrameCreated()) setValueChanged();
                    }
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        if(getFrameCreated()) setValueChanged();
                    }
                    private void setValueChanged(){
                        setFrameCreated(false);
                        searchPopUp.removeAll();
                        searchPopUp.setVisible(false);
                        searchThread = new Thread(){
                            public void run(){
                            String[] fieldArray = fieldObj.getText().split(" ");
                            int arrayLen = fieldArray.length;
                            if(!fieldObj.getText().replace(" ", "").isEmpty() && fieldArray[arrayLen-1].startsWith("?")){
                                if(fieldObj.getText().endsWith(" ")){ 
                                    setFrameCreated(true);
                                    return;
                                }
                                ArrayList<String> copyCheck = new ArrayList<String>();
                                for(String it : getResult())
                                    if(it.contains(fieldArray[arrayLen-1]) && !copyCheck.contains(it) && !it.equals(fieldArray[arrayLen-1])){
                                        searchPopUp.add(createMenuItem(it, fieldObj, fieldArray[arrayLen-1].length()));
                                        copyCheck.add(it);
                                    }
                            }
                            else if(!fieldObj.getText().startsWith("?")&&!fieldObj.getText().endsWith("?")){
                                String query = "SELECT DISTINCT ?aimVar__\n WHERE {\n";
                                if(getQueryTosearch(fieldObj).isEmpty() && !lineList.get(0).subject.equals(fieldObj)){ 
                                    setFrameCreated(true);
                                    return;
                                }
                                query += getQueryTosearch(fieldObj);
                                query += aimStr;
                                ArrayList<String> new_list  = requestProperties(endPoint, query);
                                int count =0;
                                for(String it : new_list){
                                    if(it.contains(fieldObj.getText())|| fieldObj.getText().isEmpty()) {
                                        searchPopUp.add(createMenuItem(it, fieldObj, fieldObj.getText().length()));
                                        count++; if(count >25)break;
                                    }
                                }
                            }
                            searchPopUp.show(fieldObj, fieldObj.getHorizontalAlignment()-5, fieldObj.getY()+fieldObj.getHeight());
                            setFrameCreated(true);
                            fieldObj.requestFocus();
                            }
                        };
                        searchTimer = new Thread(){
                            public void run(){
                                try{
                                    Thread.sleep(timeInMillisec);
                                    if(searchThread.isAlive()) searchThread.stop();
                                    setFrameCreated(true);
                                }
                                catch(Exception e){
                                 System.out.println("Timer Error....");
                                }
                            }
                        };
                        searchThread.start();
                        searchTimer.start();
                    }
                };
            }
            
            /**
             * Erstellt eine SPARQL-Abfrage für das Hilfemenü
             * @param obj JTextField widget
             * @return String
             */
            public String getQueryTosearch(JTextField obj){
                String query = "";
                for(SparqlArea.Line itLine : lineList){
                    for(JTextField itField : itLine.currentLine){
                        if(itField.getText().isEmpty() && !itField.equals(obj)) return "";
                        if(itField.getText().equals("?") && !itField.equals(obj)) return "";
                        if(itField.equals(obj))return query;
                        query += setToValid(setPrefix(itField.getText(), false))+" ";
                    }
                    query += ". \n";
                }
                return query;
            }
                        
            private JMenuItem createMenuItem(String str, final JTextField val, final int len){
                final JMenuItem item = new JMenuItem(str);
                item.addMouseListener(new MouseAdapter(){
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        item.isSelected();//empty statement
                    }
                });
                item.addActionListener(new ActionListener(){ 
                    public void actionPerformed(ActionEvent e) {
                        setFrameCreated(false);
                        val.setText(val.getText().substring(0, val.getText().length()-len)+item.getText());
                        setFrameCreated(true);
                        searchPopUp.setVisible(false);
                        searchPopUp.removeAll();
                        val.requestFocus();
                    } 
                });                
                return item;                 
            }
            
            /**
             * Prüft auf leere Eingabe
             * @return
             */
            public boolean isEmptyLine(){
                if(subject.getText().isEmpty() || predicate.getText().isEmpty() || object.getText().isEmpty())
                    return true;
                return false;
            }
            
        } //end Line Class
        
        SparqlArea(){
            this(14);// für Klasse FilterNotExistsArea
        }
    
        SparqlArea(int len){
            Thread thr = new Thread(){
                public void run(){
                    requestProperties(endPoint, "SELECT DISTINCT ?b_ WHERE { ?a_ ?b_ ?c_ }LIMIT 1");
                }       
            };
            thr.start(); //Beschleunigt die Ausgabe des Hilfemenüs
            lineLen = len;
            lineList = new ArrayList<Line>();
            addNewLine = new JButton("Add Line");
            addNewLine.setAlignmentY(Component.LEFT_ALIGNMENT);
            vertLineBox = Box.createVerticalBox();
            buttonBox = Box.createHorizontalBox(); 
            buttonBox.add(Box.createHorizontalStrut(22));
            buttonBox.add(addNewLine);
            buttonBox.add(Box.createHorizontalGlue());
            globVertBox = Box.createVerticalBox(); globVertBox.add(vertLineBox);
            lineList.add(new Line(vertLineBox));
            vertLineBox.add(lineList.get(0).lineBox); globVertBox.add(buttonBox);
            SparqlArea.this.add(globVertBox);
            addNewLine.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Line line = new Line(vertLineBox);
                    lineList.add(line);
                    vertLineBox.add(line.lineBox);
                    getParentFrame().setVisible(true);
                }   
            });
        }

        /**
         * Sendet eine Abfrage und gibt das Ergebnis als Liste zurück
         * @param service Endpoint
         * @param query SPARQL-Abfrage
         * @return ArrayList(Ergebnisliste)
         */
        public ArrayList<String> requestProperties(String service, String query){
             return requestProperties(service, query, "aimVar__");
         }

        /**
         * Sendet eine Abfrage und gibt das Ergebnis als Liste zurück
         * @param service Endpoint
         * @param query Abfrage 
         * @param aimVariable gezielte Variable
         * @return ArrayList(Ergebnisliste)
         */
        public ArrayList<String> requestProperties(String service, String query, String aimVariable){
            QueryExecution qe_select = QueryExecutionFactory.sparqlService(service, query);
            try {
                ResultSet results = qe_select.execSelect();
                ArrayList<String> feedback = new ArrayList<String>();
                while(results.hasNext()){
                    QuerySolution sol = results.nextSolution(); 
                    String str= setPrefix(String.valueOf(sol.get(aimVariable)));
                    feedback.add(str);
                }
                qe_select.close();
                return feedback;
                
            } catch (Exception e) {
                System.out.println("Error or Service is DOWN");
                return new ArrayList<String>();
            }
        }
        
        /**
         * gibt eine Liste von Objekteb Klasse Line zurück
         * @return Line Liste
         */
        public List<Line> getLineList(){
            return lineList;
        }
    }
    
    /**
     * Klasse FilterNotExistsArea, mit der der Benutzer Filter NOT EXISTS Anweisungen eingeben kann
     */
    public class FilterNotExistsArea extends JPanel {
        
        JButton addFilterNotExists, delFilterNotExists, moveFilterNotExists;
        SparqlArea areaFNE;
        
        FilterNotExistsArea(){
            
            addFilterNotExists = new JButton(">>");
            moveFilterNotExists = new JButton("<<");
            delFilterNotExists = new JButton("Del");
            Box addDelBox = Box.createVerticalBox();
            addDelBox.add(addFilterNotExists);
            addDelBox.add(moveFilterNotExists);
            addDelBox.add(delFilterNotExists);
            filterNotExistModel = new DefaultListModel<String>();
            filterNotExistList = new JList<String>(filterNotExistModel);
            JScrollPane filterNotExScroll = new JScrollPane(filterNotExistList);
            filterNotExScroll.setPreferredSize(new Dimension(210, 70));
            filterNotExScroll.setMaximumSize(new Dimension(210, 70));
            filterNotExistList.setAutoscrolls(true);
            Box filterNotExistBox = Box.createHorizontalBox();
            Box areaFNEverticalBox = Box.createVerticalBox();
            areaFNE = new SparqlArea(8);
            areaFNEverticalBox.add(Box.createHorizontalStrut(25));
            areaFNEverticalBox.add(areaFNE);
            setNotVisible();
            filterNotExistBox.add(areaFNEverticalBox);
            filterNotExistBox.add(addDelBox);
            filterNotExistBox.add(Box.createHorizontalStrut(10));
            filterNotExistBox.add(filterNotExScroll);
            JScrollPane filterNotExistScroll = new JScrollPane(filterNotExistBox);
            filterNotExistScroll.setPreferredSize(new Dimension(600, 110));
            filterNotExistScroll.setMaximumSize(new Dimension(600, 110));
            filterNotExistScroll.setBorder(BorderFactory.createTitledBorder("FILTER NOT EXISTS:"));
            filterNotExistBox.setAutoscrolls(true);
            add(filterNotExistScroll);
        
            addFilterNotExists.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SparqlArea.Line lineFNE = areaFNE.getLineList().get(0);
                    if(!lineFNE.isEmptyLine()){
                        filterNotExistModel.addElement(" "+setToValid(lineFNE.subject.getText())+" "
                                                   + setToValid(lineFNE.predicate.getText()) +" "
                                                   + setToValid(lineFNE.object.getText())+" ");
                    setFrameCreated(false);
                    lineFNE.subject.setText("?subject");
                    lineFNE.predicate.setText("?predicate");
                    lineFNE.object.setText("?object");
                    setFrameCreated(true);
                    }
                }
            });
        
            delFilterNotExists.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(filterNotExistList.getSelectedIndex() != -1)
                        filterNotExistModel.remove(filterNotExistList.getSelectedIndex());
                }
            });
            
            moveFilterNotExists.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(filterNotExistList.getSelectedIndex() != -1){
                        setQueryPattern(filterNotExistModel.getElementAt(filterNotExistList.getSelectedIndex()), areaFNE);
                        setNotVisible();
                        filterNotExistModel.remove(filterNotExistList.getSelectedIndex());
                    }
                }
            });
        }

        /**
         * versteckt unnötigen Widgets in View
         */
        public void setNotVisible(){
            areaFNE.getLineList().get(0).optCheck.setVisible(false);
            areaFNE.addNewLine.setVisible(false); 
            areaFNE.getLineList().get(0).delLineButton.setVisible(false);
        }
    }
    
    /**
     * Klasse RegularFilterArea, mit der der Benutzer Filterausdrücke eingeben kann
     */
    public class RegularFilterArea extends JPanel {
        
        JButton addFilter, delFilter, moveFilter;
                 
        RegularFilterArea(){
            
            addFilter = new JButton(">>");
            moveFilter = new JButton("<<");
            delFilter = new JButton("Del");
            Box addDelButtonBox = Box.createVerticalBox();
            addDelButtonBox.add(addFilter);
            addDelButtonBox.add(moveFilter);
            addDelButtonBox.add(delFilter);
            filterModel = new DefaultListModel<String>();
            filterList = new JList<String>(filterModel); 
            JScrollPane filterScrollList = new JScrollPane(filterList);
            filterScrollList.setPreferredSize(new Dimension(210, 70));
            filterScrollList.setMaximumSize(new Dimension(210, 70));
            filterList.setAutoscrolls(true);
            final SparqlArea regInput = new SparqlArea(24);
            setFrameCreated(false);
            regInput.getLineList().get(0).subject.setText("");
            regInput.getLineList().get(0).optCheck.setVisible(false);
            regInput.getLineList().get(0).predicate.setVisible(false);
            regInput.getLineList().get(0).object.setVisible(false);
            setFrameCreated(true);
            regInput.addNewLine.setVisible(false); regInput.getLineList().get(0).delLineButton.setVisible(false);
            Box vertRegInput = Box.createVerticalBox(); vertRegInput.add(Box.createVerticalStrut(25));
            vertRegInput.add(regInput);
            Box filterBox = Box.createHorizontalBox();
            filterBox.add(vertRegInput);        
            filterBox.add(addDelButtonBox);
            filterBox.add(Box.createHorizontalStrut(15));
            filterBox.add(filterScrollList);
            filterBox.add(Box.createHorizontalStrut(15));
            JScrollPane filterScroll = new JScrollPane(filterBox);
            filterScroll.setPreferredSize(new Dimension(600, 110));
            filterScroll.setMaximumSize(new Dimension(600, 110));
            filterScroll.setBorder(BorderFactory.createTitledBorder("FILTER:"));
            filterBox.setAutoscrolls(true);
            add(filterScroll);
        
            addFilter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(regInput.getLineList().get(0).subject.getText().replace(" ", "").isEmpty()) return;
                    filterModel.addElement(" "+regInput.getLineList().get(0).subject.getText()+" ");
                    setFrameCreated(false);
                    regInput.getLineList().get(0).subject.setText("");
                    setFrameCreated(true);
                }
            });
        
            delFilter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(filterList.getSelectedIndex() != -1)
                        filterModel.remove(filterList.getSelectedIndex());    
                }
            });
            
            moveFilter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(filterList.getSelectedIndex() != -1){
                        String str = regInput.getLineList().get(0).subject.getText();
                        regInput.getLineList().get(0).
                                subject.setText(str+filterModel.getElementAt(filterList.getSelectedIndex()));
                        filterModel.remove(filterList.getSelectedIndex());
                    }    
                }
            });            
        }
    }
    
    SparqlBuilder(JFrame parent, String service){
        
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
        endPoint = service;
        parentFrame = parent;
        countingThread = new Thread();
        Box globVertB = Box.createVerticalBox();
        area51 = new SparqlArea();
        JScrollPane scrollArea = new JScrollPane(area51);
        scrollArea.setPreferredSize(new Dimension(600, 300));
        scrollArea.setMinimumSize(new Dimension(600, 300));
        area51.setAutoscrolls(true); 
        globVertB.add(scrollArea);
        globVertB.add(new FilterNotExistsArea());
        globVertB.add(new RegularFilterArea());
        generate_query = new JButton("Show Query");
        info_button = new JButton("Info");
        count_request = new JButton("Count Result");
        resultLabel = new JLabel("        ");
        Box addSubBox = Box.createHorizontalBox(); 
        addSubBox.add(info_button);
        addSubBox.add(Box.createHorizontalStrut(5));
        addSubBox.add(generate_query);
        addSubBox.add(Box.createHorizontalStrut(5));
        addSubBox.add(count_request);
        addSubBox.add(Box.createHorizontalStrut(5));
        addSubBox.add(resultLabel);
        globVertB.add(addSubBox);
        globVertB.add(Box.createVerticalStrut(5));
        SparqlBuilder.this.add(globVertB);
        getParentFrame().setVisible(true);
       
        generate_query.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextArea query_area = new JTextArea(generateSPARQL());
                JOptionPane.showMessageDialog(null, new JScrollPane(query_area), "SPARQL QUERY!",
                JOptionPane.INFORMATION_MESSAGE);
            }   
        });
        
        info_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, infoLabel, "Information",
                JOptionPane.INFORMATION_MESSAGE);
            }   
        });
        count_request.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(countingThread.isAlive() && count_request.getText().equals("Stop")){ 
                    countingThread.stop();
                    count_request.setText("Start Counting");
                }
                else{
                    countingThread = new Thread(){
                        public void run(){
                            count_request.setText("Stop");
                            resultLabel.setText("");
                            int counter_result = 0;
                            try {
                                QueryExecution query_select = QueryExecutionFactory.sparqlService(endPoint, 
                                        Namespaces.getPrefix()+" "+generateSPARQL("*")+"LIMIT 20000");
                                ResultSet results = query_select.execSelect();
                                while(results.hasNext()){
                                    results.nextSolution(); 
                                    counter_result++;
                                }
                            query_select.close();
                            } catch (Exception e) {
                                System.out.println("Error or Service is DOWN");
                            }
                            String biggerAs20k = counter_result == 20000 ? "> " : "";
                            resultLabel.setText("Result: "+biggerAs20k+ String.valueOf(counter_result));
                            count_request.setText("Start Counting");
                        }
                    };
                    countingThread.start();
                }
            }   
        });
    }
    
    /**
     * Gibt das Vater JFrame zurück
     * @return
     */
    public JFrame getParentFrame(){
        return parentFrame;
    }
    
    /**
     * wird verwendet, um den Zustand des Simaphors zu bestimmen
     * @return boolesche zustand des Simaphors
     */
    public boolean getFrameCreated(){
        return frameCreated;
    }

    /**
     * Ändert den Zustand eines Semaphors
     * @param bool
     */
    public void setFrameCreated(boolean bool){
        frameCreated = bool;    
    }
    
    /**
     * Gibt SPARQL-Abfrage anhand von Benutzer eingegebenen Daten zurück
     * @return SPARQL-Abfrage
     */
    public String generateSPARQL(){
        return generateSPARQL("");
    }
    
    /**
     * Gibt SPARQL-Abfrage anhand von Benutzer eingegebenen Daten zurück
     * @param typeOfQuery Type des Abfrage("*" - Result Counting)
     * @return SPARQL-Abfrage
     */
    public String generateSPARQL(String typeOfQuery){
        StringBuilder str_query = new StringBuilder("SELECT DISTINCT");
        if(typeOfQuery.equals("*")) str_query.append(" *");
        else 
            for(String it : getResult())
                str_query.append(" " + it + " ");
        str_query.append("\n"); 
        str_query.append("WHERE {\n");
        str_query.append(getQueryPattern());
        str_query.append("}"); 
        return str_query.toString();
    }
    
    /**
     * Erstellt eine Liste aller Variablen in der Abfrage
     * @return ArrayList
     */
    public ArrayList<String> getResult(){
        ArrayList<String> result = new ArrayList();
        for(SparqlArea.Line itLine : area51.getLineList())
            for(JTextField fieldObj: itLine.getCurrentLine())
                for(String fieldStr : fieldObj.getText().split(" "))
                    if(fieldStr.startsWith("?") && !result.contains(fieldStr)){
                    result.add(fieldStr); break;
                    }
        return result;
    }
    
    /**
     * Wird beim Bearbeiten einer Regel verwendet. Unterteilt die Anfrage und fügt sie in die entsprechenden Widgets ein
     * @param str das Abfrage
     * @param areaObj Klasse wo wird das Anfrage eingefügt
     */
    public void setQueryPattern(String str, SparqlArea areaObj){
        setFrameCreated(false);
        areaObj.lineList.get(0).delLineButton.doClick();
        for(String it : str.split("\n")){
            if(it.isEmpty() || it.replace(" ", "").isEmpty()) continue;
            it = stripString(it, " ");
            if(it.contains("FILTER NOT EXISTS {") || it.contains("MINUS {") || it.contains("}")){
                it = it.replace("FILTER NOT EXISTS {", "").replace("MINUS {", "")
                        .replace("{", "").replace("}", "").replace(".", "!");
                for(String filterNotEx : it.split("!"))
                    if(!filterNotEx.replace(" ", "").isEmpty())filterNotExistModel.addElement(filterNotEx);
            }
            else if(it.contains("FILTER (")){
                it = it.replace(" . ", " ! ").replace(". ", " ! ");
                for(String filterStr : it.split(" ! ")){
                    filterStr = filterStr.replace("FILTER (", "");
                    filterStr = filterStr.substring(0, filterStr.lastIndexOf(")"));
                    if(!filterStr.replace(" ", "").isEmpty())filterModel.addElement(filterStr);
                }
            }
            else{
                areaObj.addNewLine.doClick();
                int len = areaObj.lineList.size(); boolean opt = false;
                if(it.contains("OPTIONAL {")){ 
                    areaObj.lineList.get(len-1).optCheck.setSelected(true);
                    opt = true;
                }
                it = it.replace("OPTIONAL {", "").replace(" }", "");
                Iterator<JTextField> iter = areaObj.lineList.get(len-1).currentLine.iterator();
                JTextField field = iter.next(); field.setText("");
                boolean nextField = false; boolean clinch = false; boolean newrdfstate = false;
                for(String elem : it.split("")){
                    if(elem.equals(".")&& !clinch) continue;
                    if(elem.equals(" ")&& !clinch && nextField){
                        nextField = false;
                        if(iter.hasNext()){ 
                            field = iter.next(); field.setText("");
                        }
                        else{
                            newrdfstate = true;
                        }
                    }
                    else if(elem.equals(" ")&& clinch || elem.equals(".")&& clinch){
                        field.setText(field.getText()+elem);
                    }
                    else if(!elem.equals(" ")){
                        nextField = true;
                        if(newrdfstate){
                            newrdfstate = false;
                            areaObj.addNewLine.doClick();
                            if(opt) areaObj.lineList.get(areaObj.lineList.size()-1).optCheck.setSelected(true);
                            iter = areaObj.lineList.get(areaObj.lineList.size()-1).currentLine.iterator();
                            field = iter.next(); field.setText("");
                        }
                        if(elem.equals("<")||elem.equals(">")) continue;
                        if(elem.equalsIgnoreCase("\"")) clinch = !clinch;
                        else {
                            field.setText(field.getText()+elem);
                        }
                    }
                }
            }
        }
        setFrameCreated(true);
    }
    
    /**
     * Erstellt einen Körper des SPARQL-Abfrage
     * @return Körper des SPARQL-Abfrage
     */
    public String getQueryPattern(){
        String query_pattern = "";
        for(SparqlArea.Line itLine: area51.getLineList()){
            if(itLine.optCheck.isSelected()) query_pattern += "OPTIONAL {";
            query_pattern +=" " + setToValid(itLine.subject.getText()) +" "+
                           setToValid(itLine.predicate.getText()) +" "+
                              setToValid(itLine.object.getText()) + ". \n";
            if(itLine.optCheck.isSelected()){ 
                query_pattern = query_pattern.substring(0, query_pattern.length()-3);
                query_pattern += " }\n";
            }
        }
        for(int i = 0; i < filterNotExistModel.size(); i++)
                query_pattern += "FILTER NOT EXISTS {" +filterNotExistModel.getElementAt(i)+"}\n";
        for(int i = 0; i < filterModel.size(); i++)
                query_pattern += "FILTER (" +filterModel.getElementAt(i)+")\n";
     
        return query_pattern;
    }
    
    /**
     * Validiert die Eingabe
     * @param str Wert des Eingabe
     * @return Validierte Wert
     */
    public String setToValid(String str){
        if(str.replace(" ", "").startsWith("?")){ 
            for(String it : str.split(" "))
                if(it.startsWith("?"))return it;
            return str;
        }
        else if(str.contains("^^")){
            String[] state = str.replace("^^", " ").split(" ");
            return "\""+state[0]+"\""+"^^"+setToValid(state[1]);
        }
        else if(str.contains("@")){
            String[] state = str.split("@");
            return "\""+state[0]+"\""+"@"+state[1];
        }
        else if(str.contains(" ")) return "\""+str+"\"";
        else if(str.contains("file://")||str.contains("https://")||str.contains("http://")){
            return "<"+str+">";
        }
        else if(str.contains(":")) return str;
        else if(str.replace(" ", "").equals("")) return "?x___"; 
        else return "\""+str+"\"";
    }
    
    /**
     * Entfernt die ersten ch Buchstaben von str
     * @param str 
     * @param ch
     * @return geänderte String
     */
    public String stripString(String str, String ch){
        int counter = 0;
        for(String it : str.split("")){
            if(!it.equals(ch)) return str.substring(counter);
            counter++;
        }
        return str;
    }
    
    /**
     * winkt URI zu Präfixen
     * @param str Abfrage
     * @return geänderte Abfrage
     */
    public String setPrefix(String str){
        return setPrefix(str, true);
    }
    
    /**
     * winkt URI zu Präfixen
     * @param str Abfrage
     * @param direction true URI zu Prefix, false Prefix zu URI
     * @return geänderte Abfrage
     */
    public String setPrefix(String str, boolean direction){
        
        HashMap<String, String> urlToPrefix = new HashMap<String, String>();
        urlToPrefix.put("http://nomisma.org/id/", "nm:");
        urlToPrefix.put("http://nomisma.org/ontology#","nmo:");
        urlToPrefix.put("http://rdfs.org/ns/void#", "void:");
        urlToPrefix.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:");
        urlToPrefix.put("http://purl.org/vocab/bio/0.1/","bio:");
        urlToPrefix.put("http://www.cidoc-crm.org/cidoc-crm/","crm:");
        urlToPrefix.put("http://purl.org/dc/dcmitype/","dcmitype:");
        urlToPrefix.put("http://purl.org/dc/terms/","dcterms:"); 
        urlToPrefix.put("http://xmlns.com/foaf/0.1/","foaf:");
        urlToPrefix.put("http://www.w3.org/2003/01/geo/wgs84_pos#","geo:");
        urlToPrefix.put("http://www.w3.org/ns/org#","org:");
        urlToPrefix.put("http://data.ordnancesurvey.co.uk/ontology/geometry/","osgeo:");
        urlToPrefix.put("http://www.rdaregistry.info/Elements/c/","rdac:");
        urlToPrefix.put("http://www.w3.org/2004/02/skos/core#","skos:");
        urlToPrefix.put("http://www.w3.org/2001/XMLSchema#","xsd:");
        urlToPrefix.put("http://jena.apache.org/spatial#", "spatial:");
        urlToPrefix.put("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");
        
        if(direction)
            for(String key : urlToPrefix.keySet())
                str = str.replace(key, urlToPrefix.get(key));
        else 
            for(String value : urlToPrefix.values())
                for(String key : urlToPrefix.keySet())
                    if(urlToPrefix.get(key).equals(value)) str = str.replace(value, key);
        
        return str;
    }
    
    private static void createAndShowGUI(){
        JFrame frame = new JFrame("SPARQL Builder (Endpoint->"+""+")");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 620);
        frame.add(new SparqlBuilder(frame, "hier input Endpoint"));
        frame.setVisible(true);
    }
    
    /**
     *
     * @param args
     */
    public static void main(String args[]){
    
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
	        createAndShowGUI();
	    }
        });
    } 
    
}
