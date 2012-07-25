/*
* Copyright (C) 2005-2012 ManyDesigns srl.  All rights reserved.
* http://www.manydesigns.com/
*
* Unless you have purchased a commercial license agreement from ManyDesigns srl,
* the following license terms apply:
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License version 3 as published by
* the Free Software Foundation.
*
* There are special exceptions to the terms and conditions of the GPL
* as it is applied to this software. View the full text of the
* exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
* software distribution.
*
* This program is distributed WITHOUT ANY WARRANTY; and without the
* implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
* or write to:
* Free Software Foundation, Inc.,
* 59 Temple Place - Suite 330,
* Boston, MA  02111-1307  USA
*
*/

package com.manydesigns.portofino.pageactions.crud;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.manydesigns.elements.ElementsThreadLocals;
import com.manydesigns.elements.annotations.ShortName;
import com.manydesigns.elements.options.DefaultSelectionProvider;
import com.manydesigns.elements.options.DisplayMode;
import com.manydesigns.elements.options.SelectionProvider;
import com.manydesigns.elements.text.OgnlSqlFormat;
import com.manydesigns.elements.text.OgnlTextFormat;
import com.manydesigns.elements.text.QueryStringWithParameters;
import com.manydesigns.elements.text.TextFormat;
import com.manydesigns.portofino.application.Application;
import com.manydesigns.portofino.application.QueryUtils;
import com.manydesigns.portofino.logic.SelectionProviderLogic;
import com.manydesigns.portofino.model.database.*;
import com.manydesigns.portofino.pageactions.crud.configuration.CrudConfiguration;
import com.manydesigns.portofino.pageactions.crud.configuration.CrudProperty;
import com.manydesigns.portofino.pageactions.crud.configuration.SelectionProviderReference;
import com.manydesigns.portofino.reflection.TableAccessor;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Support object for standard (model-based) selection providers.
 *
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public class ModelSelectionProviderSupport implements SelectionProviderSupport {
    public static final String copyright =
            "Copyright (c) 2005-2012, ManyDesigns srl";

    public static final Logger logger =
            LoggerFactory.getLogger(ModelSelectionProviderSupport.class);

    protected final Application application;
    protected final CrudConfiguration crudConfiguration;
    protected List<CrudSelectionProvider> crudSelectionProviders;
    protected final Multimap<List<String>, ModelSelectionProvider> availableSelectionProviders;

    public ModelSelectionProviderSupport(Application application, CrudConfiguration crudConfiguration) {
        this.application = application;
        this.crudConfiguration = crudConfiguration;
        availableSelectionProviders = HashMultimap.create();
    }

    public void setup() {
        crudSelectionProviders = new ArrayList<CrudSelectionProvider>();
        Set<String> configuredSPs = new HashSet<String>();
        for(SelectionProviderReference ref : crudConfiguration.getSelectionProviders()) {
            boolean added;
            if(ref.getForeignKey() != null) {
                added = setupSelectionProvider(ref, ref.getForeignKey(), configuredSPs);
            } else if(ref.getSelectionProvider() instanceof DatabaseSelectionProvider) {
                DatabaseSelectionProvider dsp = (DatabaseSelectionProvider) ref.getSelectionProvider();
                added = setupSelectionProvider(ref, dsp, configuredSPs);
            } else {
                AbstractCrudAction.logger.error("Unsupported selection provider: " + ref.getSelectionProvider());
                continue;
            }
            if(ref.isEnabled() && !added) {
                AbstractCrudAction.logger.warn("Selection provider {} not added; check whether the fields on which it is configured " +
                        "overlap with some other selection provider", ref);
            }
        }


        //Remove disabled selection providers and mark them as configured to avoid re-adding them
        Iterator<CrudSelectionProvider> it = crudSelectionProviders.iterator();
        while (it.hasNext()) {
            CrudSelectionProvider sp = it.next();
            if(sp.getSelectionProvider() == null) {
                it.remove();
                Collections.addAll(configuredSPs, sp.getFieldNames());
            }
        }

        Table table = crudConfiguration.getActualTable();
        if(table != null) {
            for(ForeignKey fk : table.getForeignKeys()) {
                setupSelectionProvider(null, fk, configuredSPs);
            }
            for(ModelSelectionProvider dsp : table.getSelectionProviders()) {
                if(dsp instanceof DatabaseSelectionProvider) {
                    setupSelectionProvider(null, (DatabaseSelectionProvider) dsp, configuredSPs);
                } else {
                    AbstractCrudAction.logger.error("Unsupported selection provider: " + dsp);
                }
            }
        }
    }

    protected boolean setupSelectionProvider(
            @Nullable SelectionProviderReference ref,
            DatabaseSelectionProvider current,
            Set<String> configuredSPs) {
        List<Reference> references = current.getReferences();

        String[] fieldNames = new String[references.size()];
        Class[] fieldTypes = new Class[references.size()];

        int i = 0;
        for (Reference reference : references) {
            Column column = reference.getActualFromColumn();
            fieldNames[i] = column.getActualPropertyName();
            fieldTypes[i] = column.getActualJavaType();
            i++;
        }

        availableSelectionProviders.put(Arrays.asList(fieldNames), current);
        for(String fieldName : fieldNames) {
            //If another SP is configured for the same field, stop
            if(configuredSPs.contains(fieldName)) {
                return false;
            }
        }

        if(ref == null || ref.isEnabled()) {
            DisplayMode dm = ref != null ? ref.getDisplayMode() : DisplayMode.DROPDOWN;
            String newHref = ref != null ? ref.getCreateNewValueHref() : null;
            String newText = ref != null ? ref.getCreateNewValueText() : null;
            SelectionProvider selectionProvider = createSelectionProvider
                    (current, fieldNames, fieldTypes, dm, newHref, newText);

            CrudSelectionProvider crudSelectionProvider =
                new CrudSelectionProvider(selectionProvider, fieldNames, newHref, newText);
            crudSelectionProviders.add(crudSelectionProvider);
            Collections.addAll(configuredSPs, fieldNames);
            return true;
        } else {
            //To avoid automatically adding a FK later
            CrudSelectionProvider crudSelectionProvider =
                new CrudSelectionProvider(null, fieldNames, null, null);
            crudSelectionProviders.add(crudSelectionProvider);
            return false;
        }
    }

    protected SelectionProvider createSelectionProvider
            (DatabaseSelectionProvider current, String[] fieldNames,
             Class[] fieldTypes, DisplayMode dm, String newHref, String newText) {
        DefaultSelectionProvider selectionProvider;

        boolean anyActiveProperty = false;
        for(String propertyName : fieldNames) {
            CrudProperty crudProperty = findProperty(propertyName, crudConfiguration.getProperties());
            if(crudProperty != null && crudProperty.isEnabled()) {
                anyActiveProperty = true;
                break;
            }
        }
        if(!anyActiveProperty) {
            //Dummy
            selectionProvider = SelectionProviderLogic.createSelectionProvider(
                    current.getName(), 0, new Class[0], Collections.<Object[]>emptyList());
        } else {
            selectionProvider = createSelectionProvider(current, fieldNames, fieldTypes, dm);
        }
        if(selectionProvider != null) {
            if(newHref != null) {
                OgnlTextFormat tf = new OgnlTextFormat(newHref);
                newHref = tf.format(this);
                String contextPath = ElementsThreadLocals.getHttpServletRequest().getContextPath();
                if(newHref.startsWith("/") && !newHref.startsWith(contextPath)) {
                    newHref = contextPath + newHref;
                }

                tf = new OgnlTextFormat(newText);
                newText = tf.format(this);
            }
            selectionProvider.setCreateNewValueHref(newHref);
            selectionProvider.setCreateNewValueText(newText);
        }
        return selectionProvider;
    }

    public List<CrudSelectionProvider> getCrudSelectionProviders() {
        return crudSelectionProviders;
    }

    public void disableSelectionProvider(List<String> key) {
        //TODO this is a shortcut: takes the first available selection provider and disables it
        Collection<ModelSelectionProvider> selectionProviders = availableSelectionProviders.get(key);
        ModelSelectionProvider dsp = selectionProviders.iterator().next();
        SelectionProviderReference sel = makeSelectionProviderReference(dsp);
        sel.setEnabled(false);
    }

    public void configureSelectionProvider
            (List<String> key, String name, DisplayMode displayMode, String createNewHref, String createNewText) {
        Collection<ModelSelectionProvider> selectionProviders = availableSelectionProviders.get(key);
        for(ModelSelectionProvider dsp : selectionProviders) {
            if(name.equals(dsp.getName())) {
                SelectionProviderReference sel = makeSelectionProviderReference(dsp);
                sel.setDisplayMode(displayMode);
                sel.setCreateNewValueHref(createNewHref);
                sel.setCreateNewValueText(createNewText);
                break;
            }
        }
    }

    protected SelectionProviderReference makeSelectionProviderReference(ModelSelectionProvider dsp) {
        SelectionProviderReference sel = new SelectionProviderReference();
        if(dsp instanceof ForeignKey) {
            sel.setForeignKeyName(dsp.getName());
        } else {
            sel.setSelectionProviderName(dsp.getName());
        }
        crudConfiguration.getSelectionProviders().add(sel);
        return sel;
    }

    public Map<List<String>, Collection<String>> getAvailableSelectionProviderNames() {
        Multimap<List<String>, String> namesMap = HashMultimap.create();
        for(Map.Entry<List<String>, ModelSelectionProvider> e : availableSelectionProviders.entries()) {
            namesMap.put(e.getKey(), e.getValue().getName());
        }
        return namesMap.asMap();
    }

    protected CrudProperty findProperty(String name, List<CrudProperty> properties) {
        for(CrudProperty p : properties) {
            if(p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    protected DefaultSelectionProvider createSelectionProvider(
            DatabaseSelectionProvider current, String[] fieldNames, Class[] fieldTypes, DisplayMode dm) {
        DefaultSelectionProvider selectionProvider = null;
        String name = current.getName();
        String databaseName = current.getToDatabase();
        String sql = current.getSql();
        String hql = current.getHql();
        if (sql != null) {
            Session session = application.getSession(databaseName);
            OgnlSqlFormat sqlFormat = OgnlSqlFormat.create(sql);
            String formatString = sqlFormat.getFormatString();
            Object[] parameters = sqlFormat.evaluateOgnlExpressions(this);
            QueryStringWithParameters cacheKey = new QueryStringWithParameters(formatString, parameters);
            Collection<Object[]> objects = getFromQueryCache(current, cacheKey);
            if(objects == null) {
                logger.debug("Query not in cache: {}", formatString);
                objects = QueryUtils.runSql(session, formatString, parameters);
                putInQueryCache(current, cacheKey, objects);
            }
            selectionProvider =
                    SelectionProviderLogic.createSelectionProvider(name, fieldNames.length, fieldTypes, objects);
            selectionProvider.setDisplayMode(dm);
        } else if (hql != null) {
            Database database = DatabaseLogic.findDatabaseByName(application.getModel(), databaseName);
            Table table = QueryUtils.getTableFromQueryString(database, hql);
            String entityName = table.getActualEntityName();
            Session session = application.getSession(databaseName);
            QueryStringWithParameters queryWithParameters = QueryUtils.mergeQuery(hql, null, this);

            Collection<Object> objects = getFromQueryCache(current, queryWithParameters);
            if(objects == null) {
                String queryString = queryWithParameters.getQueryString();
                Object[] parameters = queryWithParameters.getParameters();
                logger.debug("Query not in cache: {}", queryString);
                objects = QueryUtils.runHqlQuery(session, queryString, parameters);
                putInQueryCache(current, queryWithParameters, objects);
            }

            TableAccessor tableAccessor =
                    application.getTableAccessor(databaseName, entityName);
            ShortName shortNameAnnotation =
                    tableAccessor.getAnnotation(ShortName.class);
            TextFormat[] textFormats = null;
            //L'ordinamento e' usato solo in caso di chiave singola
            if (shortNameAnnotation != null && tableAccessor.getKeyProperties().length == 1) {
                textFormats = new TextFormat[] {
                    OgnlTextFormat.create(shortNameAnnotation.value())
                };
            }

            selectionProvider = SelectionProviderLogic.createSelectionProvider
                    (name, objects, tableAccessor.getKeyProperties(), textFormats);
            selectionProvider.setDisplayMode(dm);

            if(current instanceof ForeignKey) {
                selectionProvider.sortByLabel();
            }
        } else {
            logger.warn("ModelSelection provider '{}': both 'hql' and 'sql' are null", name);
        }
        return selectionProvider;
    }

    protected void putInQueryCache(
            DatabaseSelectionProvider sp, QueryStringWithParameters queryWithParameters, Collection objects) {}

    protected Collection getFromQueryCache(
            DatabaseSelectionProvider sp, QueryStringWithParameters queryWithParameters) {
        return null;
    }
}
