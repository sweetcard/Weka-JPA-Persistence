package org.weka.jpa;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.weka.jpa.utils.CallbackField;
import org.weka.jpa.utils.CallbackFieldToNumber;
import org.weka.jpa.utils.CallbackFieldToString;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class Weka2JPAHelper<E> {

	private Logger log;

	EntityManager em;

	/**
	 * Armazena as classes que devem ser ignoradas quando definem um campo da
	 * entidade base.
	 */
	Set<Class<?>> ignoreFieldsTypeOf = new HashSet<>();

	/**
	 * Armazena os nomes dos campos que devem ser ignorados quando definem um
	 * campo da entidade base
	 */
	Set<String> ignoreFieldsName = new HashSet<>();

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
	Map<String, CallbackField<?>> baseClassFieldCallBack = new HashMap<>();

	/**
	 * Mapa de CallBacks para manipulação de classes para uso com Relações mais
	 * complexas de Istancias.
	 * 
	 * Caso um campo não tenha um callback informado para ele quando cadastrado
	 * como extrafield, este callback pode ser usado.
	 * 
	 */
	Map<Class, CallbackField<?>> baseClassFieldClassCallBack = new HashMap<>();

	/**
	 * Armazena os campos extras para serem adicionados ao arquivo ARFF com
	 * referencia a classe base.
	 * 
	 * veja mais detalhes no método
	 * {@link #addExtraField(String, Object, CallbackField)}
	 */
	Set<String> baseClassExtraFieldsNames = new HashSet<>();

	/**
	 * Armazena o valor padrão para cada campo extra.
	 * 
	 * Veja mais detalhes no método
	 * {@link #addExtraField(String, Object, CallbackField)}.
	 */
	Map<String, Object> baseClassDefaultValuesExtraField = new HashMap<>();

	/**
	 * Armazena o valor que será tratado como incóginito (missing) para o campo
	 * da entidade base.
	 */
	Map<String, Object> mapMissingFields = new HashMap<>();

	/**
	 * Armazena o classe do valor default
	 */
	Map<String, Class<?>> baseClassDefaultClassExtraField = new HashMap<>();

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
	public Weka2JPAHelper(Logger p_logger, @Named("SocialSLA") EntityManager p_em) {
		log = p_logger;
		em = p_em;
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
		Instances l_data = createAttributesAndInstances(p_entityClass, p_list);

		ArffSaver saver = new ArffSaver();
		saver.setInstances(l_data);
		saver.setFile(p_file);

		saver.writeBatch();
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

		Instances l_data = createAttributesAndInstances(p_entityClass, null);

		ArffSaver saver = new ArffSaver();
		saver.setInstances(l_data);
		saver.setFile(p_file);

		saver.writeBatch();
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

		Weka2JPAAttributeProcessor<E> l_processor = new Weka2JPAAttributeProcessor<E>(p_entityClass, this);

		ArrayList<Attribute> l_atts = l_processor.createAttributes();

		Instances l_data = populateInstanceWithData(l_processor, l_atts, p_list);

		return l_data;
	}

	private Instances populateInstanceWithData(Weka2JPAAttributeProcessor<E> l_processor, ArrayList<Attribute> p_atts,
			Collection<E> p_list) {

		Collection<E> l_list;
		if (p_list == null) {
			log.info("Instancias obtidos diretamente pelo JPA");
			String l_qlString = "SELECT E FROM " + l_processor.getRelationBaseName() + " E ";

			Query l_query = em.createQuery(l_qlString);

			l_list = l_query.getResultList();
		} else {
			log.info("Instancias usando lista de entidades fornecida");
			l_list = p_list;
		}

		Instances l_instances = l_processor.createInstances(p_atts, l_list);

		return l_instances;
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
	public void ignoreFieldsName(List<String> p_fieldsName) {
		ignoreFieldsName.addAll(p_fieldsName);
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
	 * @see #setBaseClassNotEntity(boolean)
	 * @return
	 */
	public boolean isBaseClassNotEntity() {
		return basseClassNotEntity;
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
	 * Este método permite adicionar valores "incógnitos" (Missing) a um campos
	 * especifico.
	 * 
	 * Este método pode ser chamado quantas vezes for necessário para adicionar
	 * diversos campos como "incógnitos".
	 * 
	 * Os campso que forem "incógnitos" serão valorados com uma ? para que
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
	 * @param p_defaultValue
	 *            Valor que será observado, quando ocorrer no campo será
	 *            substituido pela interrogação no arquivo ARFF.
	 * @param p_fieldName
	 *            nome do campo que deve ser tratado como Incognito.
	 */
	public <I> void setMissing(String p_fieldName, I p_defaultValue) {
		mapMissingFields.put(p_fieldName, p_defaultValue);
	}

	/**
	 * Define um callback especifico para uma determinada classe.
	 * 
	 * Este callback pode ser usado sempre que é encontrado uma classe filha,
	 * para fornecer sua representação string no lugar de chamar o método
	 * {@link Object#toString()}
	 * 
	 * @param p_class
	 * @param p_callback
	 */
	public void setClassCallBack(@SuppressWarnings("rawtypes") Class p_class, CallbackFieldToString p_callback) {
		baseClassFieldClassCallBack.put(p_class, p_callback);
	}
}
