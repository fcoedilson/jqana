/**
 * jQana - Open Source Java(TM) code quality analyzer.
 * 
 * Copyright 2013 Cleuton Sampaio de Melo Jr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Project website: http://www.jqana.com
 */
package com.obomprogramador.tools.jqana.parsers;

import java.util.ArrayList;
import java.util.List;

import com.obomprogramador.tools.jqana.antlrparser.JavaBaseListener;
import com.obomprogramador.tools.jqana.antlrparser.JavaParser;
import com.obomprogramador.tools.jqana.context.Context;
import com.obomprogramador.tools.jqana.model.AbstractMetricParser;
import com.obomprogramador.tools.jqana.model.Measurement;
import com.obomprogramador.tools.jqana.model.defaultimpl.MetricValue;
import com.obomprogramador.tools.jqana.parsers.Member.MEMBER_TYPE;

/**
 * This is a Parser implementation that checks for LCOM4 value. It uses a
 * Listener that creates a class' members array. Then, it verifies the number of
 * "Connected components" inside the class.
 * 
 * @author Cleuton Sampaio
 * 
 */
public class Lcom4Parser extends AbstractMetricParser {

    protected List<Member> members;

    /**
     * Constructor with parameters.
     * 
     * @param packageMeasurement Measurement the package's measurement.
     * @param context Context the context to use.
     */
    public Lcom4Parser(Measurement packageMeasurement, Context context) {
        super(packageMeasurement, context, "metric.lcom4.name");

    }

    @Override
    public JavaBaseListener getListener(JavaParser p) {
        this.metricValue.setValue(1);
        this.members = new ArrayList<Member>();
        Lcom4Listener cl = new Lcom4Listener(members, p);
        return cl;
    }

    @Override
    public void afterProcessing() {
        processComponents();
    }

    private void processComponents() {
        this.measurement.setName(((Lcom4Listener) this.listener).getMainClassName());
        List<Component> connectedComponents = new ArrayList<Component>();
        for (Member m : members) {
            if (m.type == MEMBER_TYPE.METHOD) {
                int inx = members.indexOf(m);
                if (inx == (members.size() - 1)) {
                    break;
                }
                logger.debug("Analyzing method: " + m.name + " " + m.toString());
                for (int x = inx + 1; x < members.size(); x++) {
                    if (members.get(x).type == MEMBER_TYPE.METHOD) {
                        logger.debug("Checking with method: "
                                + members.get(x).name + " "
                                + members.get(x).toString());
                        checkMethodPair(connectedComponents, m, members.get(x));
                    }
                }
            }
        }

        if (this.members.size() > 0) {
            this.measurement.setName(this.members.get(0).className);
        }
        if (connectedComponents.size() > 0) {
            this.metricValue.setValue(connectedComponents.size());
        } else {
            this.metricValue.setValue(1);
        }

        this.metricValue.setViolated(this.metric.getVerificationAlgorithm()
                .verify(this.metricValue.getValue()));
        if (this.metricValue.isViolated()) {
            this.metricValue.setQtdElements(1);
        }
        updatePackageMeasurement();
        logger.debug("Connected components: " + connectedComponents.toString());
    }

    /*
     * (non javadoc) Check whether or not the pair of methods are related. If
     * they are related, then we must add them to a logical component.
     */
    private void checkMethodPair(List<Component> listC, Member m, Member n) {

        boolean connectedMethods = false;

        // Do they call each other?
        if (m.referencedMembers.contains(n) || n.referencedMembers.contains(m)) {
            // YUP
            logger.debug("Methods " + m.name + " and " + n.name
                    + " reference each other");
            connectedMethods = true;
        } else {
            // Do they share another class member?
            List<Member> mReferences = new ArrayList<Member>(
                    m.referencedMembers.size());
            mReferences.addAll(m.referencedMembers);
            mReferences.retainAll(n.referencedMembers);
            if (mReferences.size() > 0) {
                // YES! They share at least one class member
                logger.debug("Methods " + m.name + " and " + n.name
                        + " share a common reference");
                connectedMethods = true;
            }
        }

        if (connectedMethods) {
            logger.debug("Related method pair: " + m.name + " and: " + n.name);
            verifyComponent(listC, m, n);
        }
    }

    /*
     * (non javadoc) This method must add the related method pair to a logical
     * component. First, the method tries to find a Component related to any of
     * methods. If such a Component is found, then the method pair and all their
     * references are added as members of the component.
     * 
     * Three situations can happen:
     * 
     * a) A suitable component is found, so the method pair and their references
     * are added to it. b) No suitable component is found, then we need to
     * create a new one. c) More than one suitable component is found, then we
     * need to consolidate them in one new component and add the method pair and
     * all their references to it.
     */
    private void verifyComponent(List<Component> listC, Member m, Member n) {
        List<Component> listComponentsFound = new ArrayList<Component>();

        // Let's see if there is already a component to add them
        for (Component c : listC) {
            checkExists(listC, listComponentsFound, m, n);
        }

        if (listComponentsFound.size() == 0) {
            // No component found... Let's create a new one
            Component component = new Component();
            component.members = new ArrayList<Member>();
            listC.add(component);
            addToComponent(component, m, n);
            logger.debug("Creating new Component: " + component);
        } else if (listComponentsFound.size() == 1) {
            // One component found... Let's add them to it
            Component component = listComponentsFound.get(0);
            addToComponent(component, m, n);
            logger.debug("Adding to Component: " + component);
        } else {
            // Hmmm... Many components found... Let's consolidate them
            consolidateComponents(listC, listComponentsFound, m, n);
            logger.debug("Consolidating Component...");
        }
    }

    /*
     * (non javadoc) We need to create a new component, containing all the other
     * components' members and the method pair members (including references).
     */
    private void consolidateComponents(List<Component> listC,
            List<Component> listComponentsFound, Member m, Member n) {
        Component consolidated = new Component();
        consolidated.members = new ArrayList<Member>();
        for (Component duplicate : listComponentsFound) {
            addMembers(consolidated, duplicate.members);
            listC.remove(duplicate);
            logger.debug("     Adding to Consolidate component: " + duplicate);
        }
        addToComponent(consolidated, m, n);
        listC.add(consolidated);
    }

    private void addToComponent(Component component, Member m, Member n) {
        List<Member> members2add = new ArrayList<Member>(2
                + m.referencedMembers.size() + n.referencedMembers.size());
        members2add.add(m);
        members2add.add(n);
        members2add.addAll(m.referencedMembers);
        members2add.addAll(n.referencedMembers);
        addMembers(component, members2add);
    }

    private void addMembers(Component component, List<Member> members2add) {
        if (members2add != null) {
            for (Member cMember : members2add) {
                if (!component.members.contains(cMember)) {
                    component.members.add(cMember);
                }
            }
        }
    }



    /*
     * (non javadoc) Verify if the method pair is already referenced in any
     * Component, adding the Component to the list of components where the
     * method pair was found. The method pair is considered as referenced by a
     * component if any of three situations happen: a) Any of the pair's methods
     * exists as members of the component; b) Any referenced class member of any
     * of the pair's methods exists as members of the component; c) Any
     * referenced class member of any of the pair's methods exists as references
     * of any members of the component;
     */
    private void checkExists(List<Component> listC,
            List<Component> listComponentsFound, Member m, Member n) {
        Component found = null;
        for (Component c : listC) {
            if (c.members.contains(m) || c.members.contains(n)) {
                found = c;
                listComponentsFound.add(c);
            } else {

                /*
                 * If we do not find the method pair in a component, two
                 * situations can happen: 1) Their references may be members of
                 * the component, so, we consider this component as found; 2)
                 * Their references may be present in the references of the
                 * component's member, so, we consider this component as found.
                 */
                List<Member> combinedRefs = new ArrayList<Member>(
                        m.referencedMembers.size() + n.referencedMembers.size());
                combinedRefs.addAll(m.referencedMembers);
                combinedRefs.addAll(n.referencedMembers);
                if (refsAreMembers(c.members, combinedRefs)) {
                    found = c;
                    listComponentsFound.add(c);
                } else {
                    // Well, now, we have to check if any of the method pair's
                    // references exists as references of any member of this
                    // component
                    if (refsAreRefs(c.members, combinedRefs)) {
                        found = c;
                        listComponentsFound.add(c);
                    }

                }

            }
        }

    }

    private boolean refsAreRefs(List<Member> componentMembers,
            List<Member> combinedRefs) {
        boolean returnCode = false;
        for (Member m : componentMembers) {
            if (refsAreMembers(m.referencedMembers, combinedRefs)) {
                returnCode = true;
                break;
            }
        }
        return returnCode;
    }

    private boolean refsAreMembers(List<Member> originalMembers,
            List<Member> referencedMembers) {
        boolean returnCode = false;
        List<Member> testList = new ArrayList<Member>(originalMembers.size());
        testList.addAll(originalMembers);
        testList.retainAll(referencedMembers);
        if (testList.size() > 0) {
            returnCode = true;
        }
        return returnCode;
    }

    /**
     * This class aggregates a component. 
     * @author Cleuton Sampaio
     *
     */
    class Component {
        
        List<Member> members;

        /**
         * Object to string.
         * @return String the textual representation.
         */
        @Override
        public String toString() {
            String conector = "";
            String saida = "[Component: ";
            for (Member m : members) {
                saida += conector + m.name;
                conector = "; ";
            }
            saida += " ***]";
            return saida;
        }

    }

    @Override
    public void updatePackageMetrics(MetricValue packageMv, MetricValue mv) {
        // For package and project metrics, the value is the highest LCOM4 value
        // found:
        if (mv.getValue() > packageMv.getValue()) {
            packageMv.setValue(mv.getValue());
        }
        if (mv.isViolated()) {
            packageMv.setViolated(true);
        }

    }

}
