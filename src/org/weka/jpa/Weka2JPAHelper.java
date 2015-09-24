package org.weka.jpa;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.weka.jpa.utils.CallBackExtraAttributeFromFieldToString;
import org.weka.jpa.utils.CallbackField;
import org.weka.jpa.utils.CallbackFieldToNumber;
import org.weka.jpa.utils.CallbackFieldToString;
import org.weka.jpa.utils.ExtraAttributesFromFieldToString;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class Weka2JPAHelper<E> {

	private final Logger log;

	private final EntityManager em;

	/**
	 * Armazena as classes que devem ser ignoradas quando definem um campo da
	 * entidade base.
	 */
	private final Set<Class<?>> ignoreFieldsTypeOf = new HashSet<>();

	/**
	 * Armazena os nomes dos campos que devem ser ignorados quando definem um
	 * campo da entidade base
	 */
	private final Set<String> ignoreFieldsName = new HashSet<>();

	/**
	 * Flag que permite usar classes que não sejam Entitades de persistencia.
	 *
	 * Veja o método {@link #setBaseClassNotEntity(boolean)} para mais detalhes.
	 */
	private boolean basseClassNotEntity;

	/**
	 * Mapa de CallBacks para manipulação especial de campos.
	 *
	 * Um callback é responsável por manipular um campo especifico, este
	 * {@link org.weka.jpa.utils.CallbackField} trabalha especificamente com o
	 * nome do campo e se refere especificamente a um campo da classe base.
	 */
	private final Map<String, CallbackField<?>> baseClassFieldCallBack = new HashMap<>();

	/**
	 * Mapa de CallBacks para manipulação de classes para uso com Relações mais
	 * complexas de Istancias.
	 *
	 * Caso um campo não tenha um callback informado para ele quando cadastrado
	 * como extrafield, este callback pode ser usado.
	 *
	 */
	private final Map<Class, CallbackField<?>> baseClassFieldClassCallBack = new HashMap<>();

	/**
	 * Armazena os campos extras para serem adicionados ao arquivo ARFF com
	 * referencia a classe base.
	 *
	 * veja mais detalhes no método
	 * {@link #addExtraField(String, Object, CallbackField)}
	 */
	private final Set<String> baseClassExtraFieldsNames = new HashSet<>();

	/**
	 * Armazena os novos atributos que serão criados com base nos campos da
	 * entidade que é filha da entidade principal.
	 *
	 * Sua construção e feita da seguinte forma:
	 *
	 *
	 *
	 * Sendo portanto a chave do mapa o nome do Atributo. para saber se um campo
	 * está atribuido a um atributo extra deve buscar o mapa associado.
	 */
	@SuppressWarnings("rawtypes")
	private final Map<String, ExtraAttributesFromFieldToString> extraAttributesFromFieldToString = new HashMap<>();

	/**
	 * Armazena o valor padrão para cada campo extra.
	 *
	 * Veja mais detalhes no método
	 * {@link #addExtraField(String, Object, CallbackField)}.
	 */
	private final Map<String, Object> baseClassDefaultValuesExtraField = new HashMap<>();

	/**
	 * Armazena o valor que será tratado como incóginito (missing) para o campo
	 * da entidade base.
	 */
	private final Map<String, Object> mapMissingValueToFields = new HashMap<>();

	/**
	 * Armazena nomes de campos e o nome do atributo que deve ser usado.
	 *
	 * Quando definido a anotação @WekaAttribute este campo terá procedencia
	 * sobre ela.
	 */
	private final Map<String, String> mapAttribteNames = new HashMap<>();

	/**
	 * Armazena o classe do valor default
	 */
	private final Map<String, Class<?>> baseClassDefaultClassExtraField = new HashMap<>();

	/**
	 * Permite que os valores de campos que definidos como Null seja do tipo
	 * Incognito (?)
	 *
	 * O padrão é sempre usar.
	 */
	private boolean useNullLikeIncognito = true;

	private final Set<String> extraAttributesFromFieldSet = new HashSet<>();

	/**
	 *
	 */
	private final Map<Attribute, String> mapAttributeToExtraField;

	private final Map<Attribute, Field> mapAttributeToField;

	private final Map<Attribute, List<String>> mapAttributeToRefValues;

	/**
	 * Caso não se esteja usando CDI (como WELD) é preciso fornecer manualmente
	 * o Logger e EntityManager para a classe;
	 *
	 * Em containers gerenciados por CDI basta solicitar a instancia da classe e
	 * esta será injetada com o Logger correto e o EntityManager adequado.
	 *
	 * @param p_logger
	 * @param p_em
	 */
	@Inject
	public Weka2JPAHelper(Logger p_logger, @Default @Named("SocialSLA") EntityManager p_em) {
		log = p_logger;
		em = p_em;

		mapAttributeToField = new HashMap<>();
		mapAttributeToExtraField = new HashMap<>();
		mapAttributeToRefValues = new HashMap<>();
	}

	public <T> boolean addExtraAttributeFromFieldToString(String p_fieldName, Class<T> p_class, String p_attributeName,
			CallBackExtraAttributeFromFieldToString<T> p_callback) {
		if (!extraAttributesFromFieldSet.add(p_attributeName)) {
			return false;
		}

		@SuppressWarnings("unchecked")
		ExtraAttributesFromFieldToString<T> l_extraAttributes = extraAttributesFromFieldToString.get(p_fieldName);

		if (l_extraAttributes == null) {
			l_extraAttributes = new ExtraAttributesFromFieldToString<T>(p_fieldName, p_class);
			extraAttributesFromFieldToString.put(p_fieldName, l_extraAttributes);
		}

		l_extraAttributes.put(p_attributeName, p_callback);
		return true;
	}

	/**
	 * Adiciona um campo extra que seja do tipo númerico.
	 *
	 * Não precisa ser callback prem veja mais destalhes em
	 * Weka2JPAHelper#addExtraField(String, Object, Callback)
	 *
	 * @see #addExtraField(String, Object, CallbackField)
	 * @see #addExtraField(String, Object, CallbackFieldToNumber)
	 * @see #addExtraField(String, Object, CallbackFieldToString)
	 * @see #addExtraField(String, Number)
	 * @see #addExtraField(String, String)
	 *
	 * @param p_string
	 * @param p_unknow
	 */
	public void addExtraField(String p_string, Number p_unknow) {
		baseClassExtraFieldsNames.add(p_string);
		baseClassDefaultValuesExtraField.put(p_string, p_unknow);
	}

	/**
	 * Adiciona um campo extra que seja do tipo númerico.
	 *
	 * Não precisa ser callback prem veja mais destalhes em
	 * Weka2JPAHelper#addExtraField(String, Object, Callback)
	 *
	 * @see #addExtraField(String, Object, CallbackField)
	 * @see #addExtraField(String, Object, CallbackFieldToNumber)
	 * @see #addExtraField(String, Object, CallbackFieldToString)
	 * @see #addExtraField(String, Number)
	 * @see #addExtraField(String, String)
	 *
	 * @param p_string
	 * @param p_unknow
	 */
	public void addExtraField(String p_string, String p_unknow) {
		baseClassExtraFieldsNames.add(p_string);
		baseClassDefaultValuesExtraField.put(p_string, p_unknow);

	}

	/**
	 * Específico para tipos de retorno Number, veja mais detalhes em
	 * {@link #addExtraFieldToString(String, Object, CallbackField)}
	 *
	 * @see #addExtraField(String, Object, CallbackField)
	 * @see #addExtraField(String, Object, CallbackFieldToNumber)
	 * @see #addExtraField(String, Object, CallbackFieldToString)
	 * @see #addExtraField(String, Number)
	 * @see #addExtraField(String, String)
	 *
	 * @param p_string
	 * @param p_unknow
	 * @param p_callback
	 */
	public <R extends Number, V> void addExtraFieldToNumber(String p_string, V p_unknow,
			CallbackFieldToNumber<R> p_callback) {
		baseClassExtraFieldsNames.add(p_string);
		baseClassDefaultValuesExtraField.put(p_string, p_unknow);
		baseClassFieldCallBack.put(p_string, p_callback);
	}

	/**
	 * Permite adicionar um campo extra a classe base.
	 *
	 * Quando for preciso adicionar um campo extra a classe base deve ser usado
	 * este método informando o valor padrão que se deseja usar neste campo.
	 *
	 * O Valor padrão pode ser qualquer tipo de objeto, porém se não for String
	 * ou Number (ou super classe de Number) deverá existir um
	 * {@link CallbackField} para manipula-lo e converte-lo em String ou Number.
	 *
	 * Examplo: <code>
	 * l_arffHelper.addExtraField("classification",new Classification(2,"?"), (p_entity, p_field,p_value)->{
	 * 			return p_value.getName();
	 *    });
	 * </code>
	 *
	 * Específico para tipos de retorno String, veja mais detalhes em
	 * {@link #addExtraField(String, Object, CallbackField)}
	 *
	 * @see #addExtraField(String, Object, CallbackField)
	 * @see #addExtraField(String, Object, CallbackFieldToNumber)
	 * @see #addExtraField(String, Object, CallbackFieldToString)
	 * @see #addExtraField(String, Number)
	 * @see #addExtraField(String, String)
	 *
	 * @param p_string
	 * @param p_unknow
	 * @param p_callback
	 */
	public <V> void addExtraFieldToString(String p_string, V p_unknow, CallbackFieldToString p_callback) {
		baseClassExtraFieldsNames.add(p_string);
		baseClassDefaultValuesExtraField.put(p_string, p_unknow);
		baseClassFieldCallBack.put(p_string, p_callback);
	}

	public boolean containFieldNameForExtraAttributesFromFieldToString(String p_fieldName) {
		return extraAttributesFromFieldToString.containsKey(p_fieldName);
	}

	/**
	 * Cria as instancias usadas para construir a seção dados, mas antes
	 * constroi o cabeçalho com as informações de atributos do arquivo ARFF
	 *
	 * @param p_entityClass
	 * @param p_list
	 * @return
	 */
	private Instances createAttributesAndInstances(Class<E> p_entityClass, Collection<E> p_list) {

		if (!basseClassNotEntity && !p_entityClass.isAnnotationPresent(Entity.class)) {
			throw new NotEntityWEKAJPARuntimeException();
		}

		final Weka2JPAAttributeProcessor<E> l_attributeProcessor = new Weka2JPAAttributeProcessor<E>(p_entityClass,
				this);
		final ArrayList<Attribute> l_atts = l_attributeProcessor.createAttributes();

		final Weka2JPAIntancesProcessor<E> l_instanceProcessor = new Weka2JPAIntancesProcessor<E>(p_entityClass, this);
		final Instances l_data = l_instanceProcessor.createInstances(l_atts, p_list);

		return l_data;
	}

	Query createQuery(String p_qlString) {
		return em.createQuery(p_qlString);
	}

	void forEachAttributeToExtraField(BiConsumer<? super Attribute, ? super String> p_action) {
		mapAttributeToExtraField.forEach(p_action);
	}

	void forEachAttributeToField(BiConsumer<? super Attribute, ? super Field> p_action) {
		mapAttributeToField.forEach(p_action);
	}

	/**
	 * obtem o nome a ser usado como atributo para um determinado Campo.
	 *
	 * Quando o campo for de uma classe filha usar o nome composto:
	 * "dataPost.createAt" para representa-lo.
	 *
	 * @param p_fieldName
	 * @return
	 */
	String getAttributeName(String p_fieldName) {
		return mapAttribteNames.get(p_fieldName);
	}

	CallbackField<?> getBaseClassFieldClassCallBack(Class<?> p_class) {
		return baseClassFieldClassCallBack.get(p_class);
	}

	CallbackField<?> getBaseClassFieldClassCallBack(String p_fieldName) {

		return baseClassFieldClassCallBack.get(p_fieldName);
	}

	CallbackField<?> getCallBackExtraField(String p_fieldName) {

		return baseClassFieldCallBack.get(p_fieldName);
	}

	Class<?> getDefaultClassExtraField(String p_field) {
		return baseClassDefaultClassExtraField.get(p_field);
	}

	Object getDefaultValue(String p_fieldName) {

		return baseClassDefaultValuesExtraField.get(p_fieldName);
	}

	ExtraAttributesFromFieldToString getExtraAttributesCallBackToField(String p_fieldName) {

		return extraAttributesFromFieldToString.get(p_fieldName);
	}

	Set<String> getExtraFieldsNames() {
		return baseClassExtraFieldsNames;
	}

	Object getMissingValueToFields(String p_fieldName) {

		return mapMissingValueToFields.get(p_fieldName);
	}

	List<String> getReferenceValues(Attribute p_att) {
		return mapAttributeToRefValues.get(p_att);
	}

	/**
	 * Permite informar o nome dos campos que devem ser ignorados.
	 *
	 * @see ignoreFieldName(Collection<String>)
	 * @param p_fieldsName
	 */
	public void ignoreFieldName(String... p_fieldsName) {
		ignoreFieldsName(Arrays.asList(p_fieldsName));
	}

	/**
	 * Permite informar uma colleção de nomes de campos que devem ser ignorados.
	 *
	 * @see ignoreFieldName(String...)
	 * @param p_fieldsName
	 */
	void ignoreFieldsName(List<String> p_fieldsName) {
		ignoreFieldsName.addAll(p_fieldsName);
	}

	boolean ignoreFieldsName(String p_nameField) {
		return ignoreFieldsName.contains(p_nameField);
	}

	boolean ignoreFieldsTypeOf(Class<?> p_typeField) {

		return ignoreFieldsTypeOf.contains(p_typeField);
	}

	/**
	 * Permite informar classes par que os campos que forem do mesmo tipo sejam
	 * ignorados.
	 *
	 * @see ignoreFieldTypeOf(Collection<Class>)
	 * @param p_classes
	 */
	public void ignoreFieldTypeOf(Class<?>... p_classes) {
		ignoreFieldTypeOf(Arrays.asList(p_classes));
	}

	/**
	 * Permite informar uma coleção de classes para que os campos que forem do
	 * mesmo tipo em cada classe informada sejam ignorados.
	 *
	 * @see ignoreFieldTypeOf(Class...)
	 * @param p_classes
	 */
	public void ignoreFieldTypeOf(Collection<Class<?>> p_classes) {
		ignoreFieldsTypeOf.addAll(p_classes);
	}

	/**
	 * @see #setBaseClassNotEntity(boolean)
	 * @return
	 */
	public boolean isBaseClassNotEntity() {
		return basseClassNotEntity;
	}

	String putAttributeToExtraField(Attribute l_att, final String l_extraField) {
		return mapAttributeToExtraField.put(l_att, l_extraField);
	}

	Field putAttributeToField(Attribute p_att, Field p_field) {
		return mapAttributeToField.put(p_att, p_field);
	}

	List<String> putAttributeToRefValues(Attribute p_att, List<String> p_refListValue) {
		return mapAttributeToRefValues.put(p_att, p_refListValue);
	}

	/**
	 * Cria o arquivo ARFF com base na classe da entidade informada, consultando
	 * a camada de persitencia injetada pela lista de objetos.
	 *
	 * Deve ser informado o nome do arquivo a ser criado, a classe da entidade
	 * que é a base e referencia para construir o arquivo ARFF, esta classe será
	 * usada para obter informações base dos parametros do Arquivo ARFF.
	 *
	 * Se for informado a Collection esta deverá ser uma coleção de instancias
	 * da classe informada como base, caso não informado a classe base será
	 * usasda para efetuar um "SELECT" pelo JPA na camada de persistência.
	 *
	 * @param p_file
	 * @param p_entityClass
	 * @throws IOException
	 */
	public void save(File p_file, Class<E> p_entityClass) throws IOException {

		final Instances l_data = createAttributesAndInstances(p_entityClass, null);

		final ArffSaver saver = new ArffSaver();
		saver.setInstances(l_data);
		saver.setFile(p_file);

		saver.writeBatch();
	}

	/**
	 * Cria o arquivo ARFF com base na classe da entidade informada, e a lista
	 * de entidades instancianadas.
	 *
	 * Deve ser informado o nome do arquivo a ser criado, a classe da entidade
	 * que é a base e referencia para construir o arquivo ARFF, esta classe será
	 * usada para obter informações base dos parametros do Arquivo ARFF.
	 *
	 * Se for informado a Collection esta deverá ser uma coleção de instancias
	 * da classe informada como base, caso não informado a classe base será
	 * usasda para efetuar um "SELECT" pelo JPA na camada de persistência.
	 *
	 * @param p_file
	 * @param p_entityClass
	 * @param p_list
	 * @throws IOException
	 */
	public void save(File p_file, Class<E> p_entityClass, Collection<E> p_list) throws IOException {
		final Instances l_data = createAttributesAndInstances(p_entityClass, p_list);

		final ArffSaver saver = new ArffSaver();
		saver.setInstances(l_data);
		saver.setFile(p_file);

		saver.writeBatch();
	}

	/**
	 * Adiciona um novo nome para o campo, este nome será usado como o nome do
	 * atributo.
	 *
	 * Esta nome terá precedencia sobre a anotação WekaAttribute que será
	 * criada.
	 *
	 * TODO: como tratar conflitos de nomes oriundos de classes filhas? Quando o
	 * campo for de uma classe filha usar o nome composto: "dataPost.createAt"
	 * para representa-lo.
	 *
	 * @param p_oldFieldName
	 *            Nome atual do campo
	 * @param p_newAttributeName
	 *            Nome que será usado no atributo.
	 * @return
	 */
	public String setAttributeName(String p_oldFieldName, String p_newAttributeName) {
		return mapAttribteNames.put(p_oldFieldName, p_newAttributeName);
	}

	/**
	 * Permite usar classes que não sejam entidades, neste caso a classe deverá
	 * vir completamente preenchida. Ou durante uma transação obter dados em
	 * métodos Lazy.
	 *
	 * ATENÇÃO: Futuramente as classes escravas poderão ser consultadas no banco
	 * apenas se houver mapeamento para queries ou callbacks
	 *
	 * @param p_flag
	 */
	public void setBaseClassNotEntity(boolean p_flag) {
		basseClassNotEntity = p_flag;
	}

	/**
	 * Define um callback especifico para converter uma determinada classe em
	 * String.
	 *
	 * Este callback pode ser usado sempre que é encontrado uma classe filha,
	 * para fornecer sua representação string no lugar de chamar o método
	 * {@link Object#toString()}
	 *
	 * @param p_class
	 * @param p_callback
	 */
	public <E extends Number> void setClassCallBackToNumber(@SuppressWarnings("rawtypes") Class p_class,
			Class<E> p_returnType, CallbackFieldToNumber<E> p_callback) {
		baseClassFieldClassCallBack.put(p_class, p_callback);
	}

	/**
	 * Define um callback especifico para converter uma determinada classe em
	 * String.
	 *
	 * Este callback pode ser usado sempre que é encontrado uma classe filha,
	 * para fornecer sua representação string no lugar de chamar o método
	 * {@link Object#toString()}
	 *
	 * @param p_class
	 * @param p_callback
	 */
	public void setClassCallBackToString(@SuppressWarnings("rawtypes") Class p_class,
			CallbackFieldToString p_callback) {
		baseClassFieldClassCallBack.put(p_class, p_callback);
	}

	/**
	 * Este método permite adicionar valores "incógnitos" (Missing) a um campos
	 * especifico.
	 *
	 * Este método pode ser chamado quantas vezes for necessário para adicionar
	 * diversos campos como "incógnitos".
	 *
	 * Os campos que forem "incógnitos" serão valorados com uma ? para que
	 * durante o processo com o WEKA seja tratados adequadmente.
	 *
	 * Normalmente os campos extras são tratados como "incógnitos", portanto o
	 * ideal é fornecer o mesmo objeto que foi entregue como sendo o objeto
	 * padrão do campo extra.
	 *
	 * A melhor forma de se fazer uso deste recurso é fornecer a string "?" como
	 * valor padrão do campo extra.
	 *
	 * E similar a chamar {@link Instance#setMissing(int)}.
	 *
	 * TODO: permitir adicionar mais de um valor para o mesmo campo para ser
	 * considerado Incógnito.
	 *
	 * @param p_defaultValue
	 *            Valor que será observado, quando ocorrer no campo será
	 *            substituido pela interrogação no arquivo ARFF.
	 * @param p_fieldName
	 *            nome do campo que deve ser tratado como Incognito.
	 */
	public <I> void setMissing(String p_fieldName, I p_defaultValue) {
		mapMissingValueToFields.put(p_fieldName, p_defaultValue);
	}

	/**
	 * permite definir se os valores dos campos que retornarem null sejam do
	 * tipo Incognito (?)
	 *
	 * @param p_flag
	 */
	public void setNullLikeIncognito(boolean p_flag) {
		useNullLikeIncognito = p_flag;
	}

	int sizeOfExtraAttributesFromFieldToString() {
		return extraAttributesFromFieldToString.size();
	}

	boolean useNullLikeIncognito() {
		return useNullLikeIncognito;
	}

	Collection<ExtraAttributesFromFieldToString> valuesFromExtraAttributesFromFieldToString() {
		return extraAttributesFromFieldToString.values();
	}
}
