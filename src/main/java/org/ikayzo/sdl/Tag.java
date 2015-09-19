/*
 * Simple Declarative Language (SDL) for Java
 * Copyright 2005 Ikayzo, inc.
 *
 * This program is free software. You can distribute or modify it under the 
 * terms of the GNU Lesser General Public License version 2.1 as published by  
 * the Free Software Foundation.
 *
 * This program is distributed AS IS and WITHOUT WARRANTY. OF ANY KIND,
 * INCLUDING MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, contact the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.ikayzo.sdl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>SDL (Simple Declarative Language) documents are made up of Tags.  Tags
 * contain</p>
 * 
 * <pre>
 *      a name (if not present, the name "content" is used)
 *      a namespace (optional)
 *      0 or more values (optional)
 *      0 or more attributes (optional)
 *      0 or more children (optional)
 * </pre>
 * 
 * <p>For the SDL code:</p>
 * <pre>
 *     size 4
 *     smoker false
 * </pre>
 * 
 * <p>
 * Assuming this code is in a file called values.sdl, the values can be read 
 * using the following code (ignoring exceptions):<p>
 * 
 * <pre>
 *     Tag root = new Tag("root").read(new File("values.sdl"));
 *     int size = root.getChild("size").intValue();
 *     boolean smoker = root.getChild("smoker").booleanValue();
 * </pre>
 * 
 * <p>A tag is basically a data structure with a list of values, a map of
 * attributes, and (if it has a body) child tags.  In the example above, the
 * "values.sdl" file is read into a tag called "root".  It has two children
 * (tags) called "size" and "smoker".  Both these children have one value, no 
 * attributes, and no bodies.<p>
 * 
 * <p>SDL is often used for simple key-value mappings.  To simplify things Tag  
 * has the methods getValue and setValue which operate on the first element in 
 * the values list.  Also notice SDL understands types which are determined
 * using type inference.</p>
 * 
 * <p>The example above used the simple format common in property files:</p>
 * 
 * <pre>
 * name value
 * </pre>
 * 
 * <p>The full SDL tag format is:</p>
 * 
 * <pre>
 * namespace:name value_list attribute_list {
 *     children_tags
 * }
 * </pre>
 * 
 * <p>where value_list is zero or more space separated SDL literals and 
 * attribute_list is zero or more space separated (namespace:)key=value pairs. 
 * The name, namespace, and keys are SDL identifiers.  Values are SDL literals.
 * Namespace is optional for both tag names and attributes.  Tag bodies are also
 * optional.  SDL identifiers begin with a unicode letter or an underscore (_)
 * followed by zero or more unicode letters, numbers, underscores (_),
 * dashes (-) and periods (.).</p>
 * 
 * <p>SDL also supports anonymous tags which are assigned the name "content".  
 * An annoymous tag starts with a literal and is followed by zero or more 
 * additional literals and zero or more attributes.  The examples section below
 * demonstrates the use of anonymous tags.</p>
 * 
 * <p>Tags without bodies are terminated by a new line character (\n) and may be
 * continue onto the next line by placing a backslash (\) at the end of the
 * line.  Tags may be nested to an arbitrary depth.  SDL ignores all other white
 * space characters between tokens.  Although nested blocks are indented by 
 * convention, tabs have no significance in the language.</p>
 * 
 * <p>There are two ways to write String literals.</p>
 * 
 * <p>1. Starting and ending with double quotes (").  Double quotes, backslash
 * characters (\), and new lines (\n) within this type of String literal must be
 * escaped like so:</p>
 * 
 * <pre>
 *     file "C:\\folder\\file.txt"
 *     say "I said \"something\""
 * </pre>
 * 
 * <p>This type of String literal can be continued on the next line by placing a
 * backslash (\) at the end of the line like so:</p>
 * 
 * <pre>
 *     line "this is a \
 *          long string of text"
 * </pre>
 * 
 * <p>White space before the first character in the second line will be ignored.
 * </p>
 * 
 * <p>2. Starting and ending with a backquote (`).  This type of string literal
 * can only be ended with a second backquote (`).  It is not necessary (or
 * possible) to escape any type of character within a backquote string literal.
 * This type of literal can also span lines.  All white space is preserved
 * including new lines.
 * </p>
 * 
 * <p>Examples:</p>
 * <pre>
 *     file `C:\folder\file.txt`
 *     say `I said "something"`
 *     regex `\w+\.suite\(\)`
 *     long_line `This is
 *         a long line
 *         fee fi fo fum`
 * </pre>
 * 
 * <p>Note: SDL interprets new lines in `` String literals as a single new line
 * character (\n) regarless of the platform.</p>
 * 
 * <p>Binary literals use base64 characters enclosed in square brackets ([]).
 * The binary literal type can also span lines.  White space is ignored.
 * </p>
 * 
 * <p>Examples:</p>
 * <pre>
 *     key [sdf789GSfsb2+3324sf2] name="my key"
 *     image [
 *         R3df789GSfsb2edfSFSDF
 *         uikuikk2349GSfsb2edfS
 *         vFSDFR3df789GSfsb2edf
 *     ]
 *     upload from="ikayzo.com" data=[
 *         R3df789GSfsb2edfSFSDF
 *         uikuikk2349GSfsb2edfS
 *         vFSDFR3df789GSfsb2edf
 *     ]
 * </pre>
 * 
 * <p>SDL supports date, time span, and date/time literals.  Data and Date/Time
 * literals use a 24 hour clock (0-23).  If a timezone is not specified, the
 * default locale's timezone will be used.
 * </p>
 *
 * <p>Examples:</p>
 * <pre>
 *     # create a tag called "date" with a date value of Dec 5, 2005
 *     date 2005/12/05
 *     
 *     # various time span literals
 *     hours 03:00:00
 *     minutes 00:12:00
 *     seconds 00:00:42
 *     short_time 00:12:32.423 # 12 minutes, 32 seconds, 423 milliseconds
 *     long_time 30d:15:23:04.023 # 30 days, 15 hours, 23 mins, 4 secs, 23 millis 
 *     before -00:02:30 # 2 hours and 30 minutes ago
 *     
 *     # a date time literal
 *     in_japan 2005/12/05 14:12:23.345-JST    
 * </pre>
 * 
 * <p>SDL 1.0 has thirteen literal types (parenthesis indicate optional
 * components)</p>
 * <pre>
 *     1. string (unicode) - examples: "hello" or `aloha`
 *     2. character (unicode) - example: '/'
 *            Note: &#92;uXXXX style unicode escapes are not supported (or
 *            needed because sdl files are UTF8)
 *     3. integer (32 bits signed) - example: 123
 *     4. long integer (64 bits signed) - examples: 123L or 123l
 *     5. float (32 bits signed) - examples 123.43F 123.43f 
 *     6. double float (64 bits signed) - example: 123.43 or 123.43d or 123.43D
 *     7. decimal - example: 123.44BD or 123.44bd
 *     8. boolean - examples: true or false or on or off
 *     9. date yyyy/mm/dd - example 2005/12/05
 *     10. date time yyyy/mm/dd hh:mm(:ss)(.xxx)(-ZONE)
 *            example - 2005/12/05 05:21:23.532-JST
 *            notes: uses a 24 hour clock (0-23), only hours and minutes are
 *                   mandatory
 *     11. time span using the format (d:)hh:mm:ss(.xxx)
 *            notes: if the day component is included it must be suffixed with
 *                   a lower case 'd'
 *            examples 12:14:42 (12 hours, 14 minutes, 42 seconds)
 *                     00:09:12 (9 minutes, 12 seconds)
 *                     00:00:01.023 (1 second, 23 milliseconds)
 *                     23d:05:21:23.532 (23 days, 5 hours, 21 minutes,
 *                         23 seconds, 532 milliseconds)
 *     12. binary [base64] exmaple - [sdf789GSfsb2+3324sf2]
 *     13. null
 * </pre>
 * 
 * <p>* Timezones must be specified using a valid time zone ID
 * (ex. America/Los_Angeles), three letter abbreviation (ex. HST),
 * or GMT(+/-)hh(:mm) formatted custom timezone (ex. GMT+02 or GMT+02:30)</p>
 * 
 * <p>These types are designed to be portable across Java, .NET, and other 
 * popular platforms.</p>
 * 
 * <p>
 * SDL supports four comment types.
 * <ol>
 * <li>// single line comments identicle to those used in Java, C, etc. // style
 * comments can occur anywhere in a line.  All text after // up to the new line
 * will be ignored.</li>
 * <li># property style comments.  They work the same way as //</li>
 * <li>-- separator comments useful for visually dividing content.  They work
 * the same way as //</li>
 * <li>Slash star (/*) style multiline comments.  These begin with a slash
 * star and end with a star slash.  Everything in between is ignored.</li>
 * </ol>
 * </p>
 * 
 * <p>A example SDL file:</p>
 * 
 * <pre>
 *     # a tag having only a name
 *     my_tag
 * 
 *     # three tags acting as name value pairs
 *     first_name "Akiko"
 *     last_name "Johnson"
 *     height 68
 *     
 *     # a tag with a value list
 *     person "Akiko" "Johnson" 68
 *     
 *     # a tag with attributes
 *     person first_name="Akiko" last_name="Johnson" height=68
 *     
 *     # a tag with values and attributes
 *     person "Akiko" "Johnson" height=60
 *     
 *     # a tag with attributes using namespaces
 *     person name:first-name="Akiko" name:last-name="Johnson"
 *     
 *     # a tag with values, attributes, namespaces, and children
 *     my_namespace:person "Akiko" "Johnson" dimensions:height=68 {
 *         son "Nouhiro" "Johnson"
 *         daughter "Sabrina" "Johnson" location="Italy" {
 *             hobbies "swimming" "surfing"
 *             languages "English" "Italian"
 *             smoker false
 *         }
 *     }   
 *     
 *     ------------------------------------------------------------------
 *     // (notice the separator style comment above...)
 *     
 *     # a log entry
 *     #     note - this tag has two values (date_time and string) and an 
 *     #            attribute (error)
 *     entry 2005/11/23 10:14:23.253-GMT "Something bad happened" error=true
 *     
 *     # a long line
 *     mylist "something" "another" true "shoe" 2002/12/13 "rock" \
 *         "morestuff" "sink" "penny" 12:15:23.425
 *     
 *     # a long string
 *     text "this is a long rambling line of text with a continuation \
 *        and it keeps going and going..."
 *        
 *    # anonymous tag examples
 *    
 *    files {
 *        "/folder1/file.txt"
 *        "/file2.txt"
 *    }
 *        
 *    # To retrieve the files as a list of strings
 *    #
 *    #     List files = tag.getChild("files").getChildrenValues("content");
 *    # 
 *    # We us the name "content" because the files tag has two children, each of 
 *    # which are anonymous tags (values with no name.)  These tags are assigned
 *    # the name "content"
 *        
 *    matrix {
 *        1 2 3
 *        4 5 6
 *    }
 *    
 *    # To retrieve the values from the matrix (as a list of lists)
 *    #
 *    #     List rows = tag.getChild("matrix").getChildrenValues("content");
 *        
 * </pre>
 * 
 * <p>Example of getting the "location" attribute from the "daughter" tag
 * above (ignoring exceptions)</p>
 *  
 *  <pre>
 *      Tag root = new Tag("root").read("myfile.sdl");
 *      Tag daughter = root.getChild("daughter", true); // recursive search
 *      String location = daughter.getAttribute("location").toString();
 *  </pre>
 * 
 * <p>SDL is normally stored in a file with the .sdl extension.  These files 
 * should always be encoded using UTF8.  SDL fully supports unicode in 
 * identifiers and literals.</p>
 * 
 * @author Daniel Leuck
 */
@SuppressWarnings("unchecked")
public class Tag implements Serializable {

	// TODO clearAttributes() & clearAttributes(namespace) (v1.1)
	
	// TODO SDL Schema (v2)
	// TODO SDL Ref type (v2)

	private static final long serialVersionUID = 8283229161742794620L;

	private String namespace = "";

	private String name;

	private List values = new ArrayList();
	private List valuesView = Collections.unmodifiableList(values);

	private ImmutableSortedMap<String, Object> attributes =
		new ImmutableSortedMap<String, Object>(new TreeMap<String, Object>());
	private SortedMap<String, SortedMap<String, Object>> namespaces =
		new TreeMap<String, SortedMap<String, Object>>();

	private List<Tag> children = new ArrayList<Tag>();
	private List<Tag> childrenView = Collections.unmodifiableList(children);

	/**
	 * Creates an empty tag.
	 * 
	 * @param name
	 *            The name of this tag
	 * @throws IllegalArgumentException
	 *             if the name is not a legal SDL identifier. See
	 *             {@link SDL#validateIdentifier(String)}
	 */
	public Tag(String name) {
		this("", name);
	}

	/**
	 * Creates an empty tag in the given namespace. If the namespace is null it
	 * will be coerced to an empty String.
	 * 
	 * @param namespace
	 *            The namespace for this tag
	 * @param name
	 *            The name of this tag
	 * @throws IllegalArgumentException
	 *             if the name is not a legal SDL identifier (see
	 *             {@link SDL#validateIdentifier(String)}) or the namespace is
	 *             non-blank and is not a legal SDL identifier.
	 */
	public Tag(String namespace, String name) {
		if (namespace == null)
			namespace = "";
		if (namespace.length() != 0)
			SDL.validateIdentifier(namespace);
		this.namespace = namespace;

		if (name == null || name.trim().length() == 0)
			throw new IllegalArgumentException("Tag name cannot be null or "
					+ "empty.");
		SDL.validateIdentifier(name);

		this.name = name;
	}

	/**
	 * Add a child to this Tag.
	 * 
	 * @param child
	 *            The child to add
	 */
	public void addChild(Tag child) {
		children.add(child);
	}

	/**
	 * Remove a child from this Tag
	 * 
	 * @param child
	 *            The child to remove
	 * @return true if the child exists and is removed
	 */
	public boolean removeChild(Tag child) {
		return children.remove(child);
	}

	/**
	 * A convenience method that sets the first value in the value list. See
	 * {@link #addValue(Object)} for legal types.
	 * 
	 * @param value
	 *            The value to be set.
	 * @throws IllegalArgumentException
	 *             if the value is not a legal SDL type
	 */
	public void setValue(Object value) {
		if (values.isEmpty())
			addValue(value);
		else
			values.set(0, SDL.coerceOrFail(value));
	}

	/**
	 * A convenience method that returns the first value.
	 * 
	 * @return The first value
	 */
	public Object getValue() {
		if (values.isEmpty())
			return null;
		else
			return values.get(0);
	}

	// //////////////////////////////////////////////////////////////////////////
	// Convenience methods for getValue
	// //////////////////////////////////////////////////////////////////////////

	/**
	 * A convenience method that returns the first value as a String
	 * 
	 * @return The value assuming a String
	 */
	public String stringValue() {
		return String.valueOf(getValue());
	}

	/**
	 * A convenience method that returns the first value as a boolean
	 * 
	 * @return The value assuming a boolean
	 */
	public boolean booleanValue() {
		return ((Boolean) getValue()).booleanValue();
	}

	/**
	 * A convenience method that returns the first value as an int
	 * 
	 * @return The value assuming an int
	 */
	public int intValue() {
		return ((Number) getValue()).intValue();
	}

	/**
	 * Get the first child with the given name. The search is not recursive.
	 * 
	 * @param childName
	 *            The name of the child Tag
	 * @return The first child tag having the given name or null if no such
	 *         child exists
	 */
	public Tag getChild(String childName) {
		return getChild(childName, false);
	}

	/**
	 * Get the first child with the given name, optionally using a recursive
	 * search.
	 * 
	 * @param childName
	 *            The name of the child Tag
	 * @return The first child tag having the given name or null if no such
	 *         child exists
	 */
	public Tag getChild(String childName, boolean recursive) {
		for (Tag t : children) {
			if (t.getName().equals(childName))
				return t;

			if (recursive) {
				Tag rc = t.getChild(childName, true);
				if (rc != null)
					return rc;
			}
		}

		return null;
	}

	/**
	 * Get all children with the given name. The search is not recursive.
	 * 
	 * @param childName
	 *            The name of the children to fetch
	 * @return All the child tags having the given name
	 */
	public List<Tag> getChildren(String childName) {
		return getChildren(childName, false);
	}

	/**
	 * Get all the children with the given name (optionally searching
	 * descendants recursively)
	 * 
	 * @param childName
	 *            The name of the children to fetch
	 * @param recursive
	 *            If true search children recursively in all child tags
	 * @return All the child tags having the given name
	 */
	public List<Tag> getChildren(String childName, boolean recursive) {
		List<Tag> kids = new ArrayList<Tag>();
		for (Tag t : children) {
			if (t.getName().equals(childName))
				kids.add(t);

			if (recursive)
				kids.addAll(t.getChildren(childName, true));
		}

		return kids;
	}

	/**
	 * Get all children in the given namespace. The search is not recursive.
	 * 
	 * @param namespace
	 *            The namespace to search
	 * @return All the child tags in the given namespace
	 */
	public List<Tag> getChildrenForNamespace(String namespace) {
		return getChildrenForNamespace(namespace, false);
	}

	/**
	 * Get all the children in the given namespace (optionally searching
	 * descendants recursively)
	 * 
	 * @param namespace
	 *            The namespace of the children to fetch
	 * @param recursive
	 *            If true search all descendents
	 * @return All the child tags in the given namespace
	 */
	public List<Tag> getChildrenForNamespace(String namespace, boolean recursive) {

		List<Tag> kids = new ArrayList<Tag>();
		for (Tag t : children) {
			if (t.getNamespace().equals(namespace))
				kids.add(t);

			if (recursive)
				kids.addAll(t.getChildrenForNamespace(namespace, true));
		}

		return kids;
	}

	/**
	 * Get the values for all children with the given name. If the child has
	 * more than one value, all the values will be added as a list. If the child
	 * has no value, null will be added. The search is not recursive.
	 * 
	 * @param name
	 *            The name of the children from which values are retrieved
	 * @return A list of values (or lists of values)
	 */
	public List getChildrenValues(String name) {
		ArrayList results = new ArrayList();

		List<Tag> children = getChildren(name);

		for (Tag c : children) {
			List values = c.getValues();
			if (values.isEmpty())
				results.add(null);
			else if (values.size() == 1)
				results.add(values.get(0));
			else
				results.add(values);
		}

		return results;
	}
	
	/**
	 * Checks for the presence of a child with the given name.
	 * 
	 * @param name The child's name
	 * @return true If the child exists
	 */
	public boolean hasChild(String name) {
		return getChild(name)!=null;
	}
	
	/**
	 * Checks for children
	 * 
	 * @return true If this tag has children
	 */
	public boolean hasChildren() {
		return !children.isEmpty();
	}

	/**
	 * Add a value to this Tag. The allowable types are String, Number, Boolean,
	 * Character, byte[], Byte[] (coerced to byte[]), Calendar, Date (coerced to
	 * Calendar), and null. Passing any other type will result in an
	 * IllegalArgumentException.
	 * 
	 * @param value
	 *            The value to add
	 * @throws IllegalArgumentException
	 *             if the value is not a legal SDL type
	 */
	public void addValue(Object value) {
		values.add(SDL.coerceOrFail(value));
	}

	/**
	 * Remove a value from this Tag.
	 * 
	 * @param value
	 *            The value to remove
	 * @return true If the value exists and is removed
	 */
	public boolean removeValue(Object value) {
		return values.remove(value);
	}

	/**
	 * Get all the values for this Tag.
	 * 
	 * @return An immutable view of the values.
	 */
	public List<Object> getValues() {
		return valuesView;
	}

	/**
	 * Set the values for this tag. See {@link #addValue(Object)} for legal
	 * value types.
	 * 
	 * @param values
	 *            The new values
	 * @throws IllegalArgumentException
	 *             if the collection contains any values which are not legal SDL
	 *             types
	 */
	public void setValues(Collection values) {
		this.values.clear();
		if (values != null) {
			// this is required to ensure validation of types
			for (Object o : values)
				addValue(o);
		}
	}
	
	/**
	 * Returns true if the value exists.
	 * 
	 * @param value The value
	 * @return true If the value exists.
	 */
	public boolean hasValue(Object value) {
		return values.contains(value);
	}
	
	/**
	 * Clears all values
	 */
	public void clearValues() {
		values.clear();
	}
	
	/**
	 * Set an attribute for this tag in the default namespace. The allowable
	 * attribute value types are the same as those allowed for
	 * {@link #addValue(Object)}
	 * 
	 * @param key
	 *            The attribute key
	 * @param value
	 *            The attribute value
	 * @throws IllegalArgumentException
	 *             if the key is not a legal SDL identifier (see
	 *             {@link SDL#validateIdentifier(String)}) or the value is not
	 *             a legal SDL type.
	 */
	public void setAttribute(String key, Object value) {
		setAttribute("", key, value);
	}

	/**
	 * Set an attribute in the given namespace for this tag. The allowable
	 * attribute value types are the same as those allowed for
	 * {@link #addValue(Object)}
	 * 
	 * @param namespace
	 *            The namespace for this attribute
	 * @param key
	 *            The attribute key
	 * @param value
	 *            The attribute value
	 * @throws IllegalArgumentException
	 *             if the key is not a legal SDL identifier (see
	 *             {@link SDL#validateIdentifier(String)}), or the namespace is
	 *             non-blank and is not a legal SDL identifier, or the value is
	 *             not a legal SDL type
	 */
	public void setAttribute(String namespace, String key, Object value) {
		SDL.validateIdentifier(key);
		if(namespace!=null && !"".equals(namespace))
			SDL.validateIdentifier(namespace);
			
		ImmutableSortedMap<String,Object> ns = namespace(namespace);
		((SortedMap<String,Object>)((ImmutableSortedMap<String,Object>)ns)
				.delegate).put(key, SDL.coerceOrFail(value));
	}

	/**
	 * Get the attribute value associated with the given key in the default
	 * namespace.
	 * 
	 * @return The value for the key if such a key exists
	 */
	public Object getAttribute(String key) {
		return getAttribute("", key);
	}
	
	/**
	 * Get the attribute value associated with the given key in the given
	 * namespace.
	 * 
	 * @param namespace The attribute namespace
	 * @param key The key for the entry to remove
	 * 
	 * @return The value for the key if such a key exists
	 */
	public Object getAttribute(String namespace, String key) {
		if(namespace==null || "".equals(namespace))
			return attributes.get(key);
		
		SortedMap<String,Object> ns = namespaces.get(namespace);
		if(ns==null)
			return null;
		
		return ns.get(key);
	}	

	/**
	 * Checks for the presence of an attribute with the given key.
	 * 
	 * @param key The attribute key
	 * @return True if the attribute key exists
	 */
	public boolean hasAttribute(String key) {
		return attributes.containsKey(key);
	}
	
	/**
	 * Checks for the presence of an attribute with the given key in the given
	 * namespace.
	 * 
	 * @param namespace The attribute namespace
	 * @param key The attribute key
	 * @return True if the attribute key exists
	 */
	public boolean hasAttribute(String namespace, String key) {
		SortedMap<String,Object> ns = namespaces.get(namespace);
		if(ns==null)
			return false;
		return ns.containsKey(key);
	}	
	
	/**
	 * Remove the attribute value associated with the given key in the default
	 * namespace.
	 * 
	 * @param attributeKey The attribute key
	 * @return The value for the attribute key if the key exists
	 */
	public Object removeAttribute(String attributeKey) {
		return removeAttribute("", attributeKey);
	}
	
	/**
	 * Remove the attribute value associated with the given key in the given
	 * namespace.
	 * 
	 * @param key The attribute key
	 * @return The value for the attribute key if the key exists
	 */
	public Object removeAttribute(String namespace, String key) {
		if(namespace==null || "".equals(namespace))
			return attributes.delegate.remove(key);
		
		ImmutableSortedMap<String,Object> ns =
			(ImmutableSortedMap<String,Object>)namespaces.get(key);
		if(ns==null)
			return null;
		
		return ns.delegate.remove(key);		
	}	

	
	
	/**
	 * Get an immutable view of the attributes in the default namespace.
	 * 
	 * @return An immutable view of the attributes.
	 */
	public SortedMap<String, Object> getAttributes() {
		return attributes;
	}
	
	/**
	 * Returns an immutable view of all the attributes in the given namespace
	 * or null if no such namespace exists.
	 * 
	 * @param namespace The attribute namespace
	 * @return An immutable view of all the attributes in the given namespace.
	 */
	public SortedMap<String, Object> getAttributes(String namespace) {
		return namespaces.get(namespace);
	}	

	/**
	 * Set all the attributes for this Tag in the default namespace. See
	 * {@link #addValue(Object)} for allowable attribute value types.
	 * 
	 * @param attributes
	 *            The new attributes
	 * @throws IllegalArgumentException
	 *             if any key in the map is not a legal SDL identifier (see
	 *             {@link SDL#validateIdentifier(String)}), or any value is not
	 *             a legal SDL type
	 */
	public void setAttributes(Map<String, Object> attributes) {
		setAttributes("", attributes);
	}

	/**
	 * Set all the attributes for this Tag in the given namespace. See
	 * {@link #addValue(Object)} for allowable attribute value types.
	 * 
	 * @param namespace The attribute namespace
	 * @param attributes The new attributes
	 * @throws IllegalArgumentException
	 *             if any key in the map is not a legal SDL identifier (see
	 *             {@link SDL#validateIdentifier(String)}), or any value is not
	 *             a legal SDL type
	 */
	public void setAttributes(String namespace, Map<String, Object> attributes) {
		ImmutableSortedMap<String, Object> ns = namespace(namespace);
		if(ns!=null)
			ns.delegate.clear();
		
		if (attributes != null) {
			// this is required to ensure validation
			for (Entry<String, Object> e : attributes.entrySet())
				setAttribute(namespace, e.getKey(), e.getValue());
		}
	}	
	
	/**
	 * Clear all attributes in the default namespace.
	 */
	public void clearAttributes() {
		attributes.delegate.clear();
	}
	
	/**
	 * Clear all attributes in the given namespace.
	 * 
	 * @param namespace The attribute namespace.
	 */
	public void clearAttributes(String namespace) {
		if(namespace==null)
			namespace="";
		
		ImmutableSortedMap<String, Object> ns =
			(ImmutableSortedMap<String, Object>)namespaces.get(namespace);
		if(ns!=null)
			ns.delegate.clear();
	}	
	
	/**
	 * Get all the children for this Tag
	 * 
	 * @return An immutable view of the children.
	 */
	public List<Tag> getChildren() {
		return childrenView;
	}
	
	/**
	 * Get the number of children owned by this tag
	 * 
	 * @return The number of children
	 */
	public int getChildCount() {
		return childrenView.size();
	}

	/**
	 * Get all the children of this tag optionally recursing through all
	 * descendents.
	 * 
	 * @param recursively
	 *            If true, recurse through all descendents
	 * @return An immutable view of the children
	 */
	public List<Tag> getChildren(boolean recursively) {
		if (!recursively)
			return childrenView;

		ArrayList<Tag> kids = new ArrayList();
		for (Tag t : children) {
			kids.add(t);

			if (recursively)
				kids.addAll(t.getChildren(true));
		}

		return Collections.unmodifiableList(kids);
	}

	/**
	 * Get the name of this Tag.
	 * 
	 * @return This Tag's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this Tag.
	 * 
	 * @param name
	 *            The name to set.
	 * @throws IllegalArgumentException
	 *             if the name is not a legal SDL identifier (see
	 *             {@link SDL#validateIdentifier(String)})
	 */
	public void setName(String name) {
		SDL.validateIdentifier(name);
		this.name = name;
	}

	/**
	 * Returns the namespace. Namespace is never null but may be empty.
	 * 
	 * @return This Tag's namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * The namespace to set. null will be coerced to the empty string.
	 * 
	 * @param namespace
	 *            The namespace to set
	 * @throws IllegalArgumentException
	 *             if the namespace is non-blank and is not a legal SDL
	 *             identifier (see {@link SDL#validateIdentifier(String)})
	 */
	public void setNamespace(String namespace) {
		if (namespace == null)
			namespace = "";

		if (namespace.length() != 0)
			SDL.validateIdentifier(namespace);

		this.namespace = namespace;
	}

	/**
	 * Add all the tags specified in the stream at the given URL to this Tag.
	 * 
	 * @param url
	 *            A UTF8 encoded .sdl file
	 * @throws IOException
	 *             If there is an IO problem reading the source
	 * @throws ParseException
	 *             If the SDL input is malformed
	 * @return This tag after adding all the children read from the reader
	 */
	public Tag read(URL url) throws IOException, SDLParseException {
		return read(new InputStreamReader(url.openStream(), "UTF8"));
	}

	/**
	 * Add all the tags specified in the given file to this Tag.
	 * 
	 * @param file
	 *            A UTF8 encoded .sdl file
	 * @throws IOException
	 *             If there is an IO problem reading the source
	 * @throws ParseException
	 *             If the SDL input is malformed
	 * @return This tag after adding all the children read from the reader
	 */
	public Tag read(File file) throws IOException, SDLParseException {
		return read(new InputStreamReader(new FileInputStream(file), "UTF8"));
	}

	/**
	 * Add all the tags specified in the given String to this Tag.
	 * 
	 * @param text
	 *            An SDL string
	 * @throws ParseException
	 *             If the SDL input is malformed
	 * @return This tag after adding all the children read from the reader
	 */
	public Tag read(String text) throws SDLParseException {
		try {
			return read(new StringReader(text));
		} catch (IOException ioe) {
			// Cannot happen
			throw new InternalError("IOExceptio reading a String");
		}
	}

	/**
	 * Add all the tags specified in the given Reader to this Tag.
	 * 
	 * @param reader
	 *            A reader containing SDL source
	 * @throws IOException
	 *             If there is an IO problem reading the source
	 * @throws ParseException
	 *             If the SDL input is malformed
	 * @return This tag after adding all the children read from the reader
	 */
	public Tag read(Reader reader) throws IOException, SDLParseException {
		List<Tag> tags = new Parser(reader).parse();
		for (Tag t : tags)
			addChild(t);
		return this;
	}

	/**
	 * Write this tag out to the given file.
	 * 
	 * @param file
	 *            The file to which we will write the children of this tag.
	 * @throws IOException
	 *             If there is an IO problem during the write operation
	 */
	public void write(File file) throws IOException {
		write(file, false);
	}

	/**
	 * Write this tag out to the given file (optionally clipping the root.)
	 * 
	 * @param file
	 *            The file to which we will write this tag
	 * @param includeRoot
	 *            If true this tag will be included in the file as the root
	 *            element, if false only the children will be written
	 * @throws IOException
	 *             If there is an IO problem during the write operation
	 */
	public void write(File file, boolean includeRoot) throws IOException {
		write(new OutputStreamWriter(new FileOutputStream(file), "UTF8"),
				includeRoot);
	}

	/**
	 * Write this tag out to the given writer (optionally clipping the root.)
	 * 
	 * @param writer
	 *            The writer to which we will write this tag
	 * @param includeRoot
	 *            If true this tag will be written out as the root element, if
	 *            false only the children will be written
	 * @throws IOException
	 *             If there is an IO problem during the write operation
	 */
	public void write(Writer writer, boolean includeRoot) throws IOException {
		String newLine = System.getProperty("line.separator");

		if (includeRoot) {
			writer.write(toString());
		} else {
			for (Iterator i = children.iterator(); i.hasNext();) {
				writer.write(String.valueOf(i.next()));
				if (i.hasNext())
					writer.write(newLine);
			}
		}

		writer.close();
	}

	/**
	 * Get a String representation of this SDL Tag. This method returns a
	 * complete description of the Tag's state using SDL (i.e. the output can be
	 * parsed by {@link #read(String)})
	 * 
	 * @return A string representation of this tag using SDL
	 */
	public String toString() {
		return toString("");
	}

	/**
	 * @param linePrefix
	 *            A prefix to insert before every line.
	 * @return A string representation of this tag using SDL
	 * 
	 * TODO: break up long lines using the backslash
	 */
	String toString(String linePrefix) {
		String newLine = System.getProperty("line.separator");

		if (linePrefix == null)
			linePrefix = "";

		StringBuilder builder = new StringBuilder(linePrefix);

		boolean skipValueSpace = false;
		if (name.equals("content") && namespace.equals("")) {
			skipValueSpace = true;
		} else {
			if (!namespace.equals(""))
				builder.append(namespace + ":");
			builder.append(name);
		}
		// output values
		if (!values.isEmpty()) {
			for (Iterator i = values.iterator(); i.hasNext();) {
				if (skipValueSpace)
					skipValueSpace = false;
				else
					builder.append(" ");
				builder.append(SDL.format(i.next()));
			}
		}

		// output attributes in the default namespace
		if (!attributes.isEmpty()) {
			for (Iterator<Entry<String, Object>> i = attributes.entrySet()
					.iterator(); i.hasNext();) {

				builder.append(" ");

				Entry<String, Object> e = i.next();
				String key = e.getKey();

				builder.append(key + "=");
				builder.append(SDL.format(attributes.get(key)));
			}
		}
		
		// output attributes in other namespaces
		if(!namespaces.isEmpty()) {
			for(Entry<String, SortedMap<String,Object>> e:namespaces.entrySet()) {
				
				String ns = e.getKey();
				SortedMap<String,Object> atts = e.getValue();
				
				for (Iterator<Entry<String, Object>> i = atts.entrySet()
						.iterator(); i.hasNext();) {

					builder.append(" ");

					Entry<String, Object> entry = i.next();
					String key = entry.getKey();

					builder.append(ns + ":").append(key + "=");
					builder.append(SDL.format(attributes.get(key)));
				}
			}		
		}

		// output children
		if (!children.isEmpty()) {
			builder.append(" {" + newLine);
			for (Tag t : children) {
				builder.append(t.toString(linePrefix + "    ") + newLine);
			}
			builder.append(linePrefix + "}");
		}

		return builder.toString();
	}

	/**
	 * Returns true if this tag (including all of its values, attributes, and
	 * children) is equivalent to the given tag.
	 * 
	 * @return true if the tags are equivalet
	 */
	public boolean equals(Object o) {
		// this is safe because toString() dumps the full state
		return o instanceof Tag && o.toString().equals(toString());
	}

	/**
	 * @return The hash (based on the output from toString())
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Returns a string containing an XML representation of this tag. Values
	 * will be represented using _val0, _val1, etc.
	 * 
	 * @return An XML String describing this Tag
	 */
	public String toXMLString() {
		return toXMLString("");
	}

	/**
	 * @param linePrefix
	 *            A prefix to insert before every line.
	 * @return A String containing an XML representation of this tag. Values
	 *         will be represented using _val0, _val1, etc.
	 */
	String toXMLString(String linePrefix) {
		String newLine = System.getProperty("line.separator");

		if (linePrefix == null)
			linePrefix = "";

		StringBuilder builder = new StringBuilder(linePrefix + "<");
		if (!namespace.equals(""))
			builder.append(namespace + ":");
		builder.append(name);

		// output values
		if (!values.isEmpty()) {
			int i = 0;
			for (Object val : values) {
				builder.append(" ");
				builder.append("_val" + i + "=\"" + SDL.format(val, false)
						+ "\"");
				i++;
			}
		}

		// output attributes in the default namespace
		if (!attributes.isEmpty()) {
			for (String key : attributes.keySet()) {
				builder.append(" ");

				builder.append(key + "=");
				builder.append("\"" + SDL.format(attributes.get(key), false)
						+ "\"");
			}
		}
		
		// output attributes in other namespaces
		if(!namespaces.isEmpty()) {
			for(Entry<String, SortedMap<String,Object>> e:namespaces.entrySet()) {
				
				String ns = e.getKey();
				SortedMap<String,Object> atts = e.getValue();
				
				for (Iterator<Entry<String, Object>> i = atts.entrySet()
						.iterator(); i.hasNext();) {

					builder.append(" ");

					Entry<String, Object> entry = i.next();
					String key = entry.getKey();

					builder.append(ns + ":").append(key + "=");
					builder.append("\"" + SDL.format(attributes.get(key), false)
							+ "\"");
				}
			}		
		}

		if (!children.isEmpty()) {
			builder.append(">" + newLine);
			for (Tag t : children) {
				builder.append(t.toXMLString(linePrefix + "    ") + newLine);
			}

			builder.append(linePrefix + "</");
			if (!namespace.equals(""))
				builder.append(namespace + ":");
			builder.append(name + ">");
		} else {
			builder.append("/>");
		}

		return builder.toString();
	}

	/**
	 * Gets the namespace, creating it if necessary.
	 */
	private ImmutableSortedMap<String,Object> namespace(String name) {
		if(name==null || name.equals(""))
			return attributes;
		
		ImmutableSortedMap<String,Object> namespace =
			(ImmutableSortedMap<String,Object>)namespaces.get(name);
		
		if(namespace==null) {
			namespace=new ImmutableSortedMap<String,Object>(
					new TreeMap<String,Object>());
			namespaces.put(name, namespace);
		}
		
		return namespace;
	}
	
	// These methods are useful for treating SDL files like Unicode properties files
	
	/**
	 * Creates a map snapshot using the children's names as keys and their values as the key's
	 * value. Note: This map is not backed by the tag. It is only a snapshot.
	 * 
	 * @return a map of this tag's children
	 */
	public Map<String,Object> getChildMap() {
		HashMap<String,Object> map = new HashMap<String,Object>();
		for(Tag child:children) {
			map.put(child.getName(), child.getValue());
		}
		return map;
	}
	
	/**
	 * Creates a map snapshot using the children's names as keys and their values as the key's
	 * value. Values are converted to Strings. Null values become empty Strings. Note: This map is
	 * not backed by the tag. It is only a snapshot.
	 * 
	 * @return a map of this tag's children
	 */
	public Map<String,String> getChildStringMap() {
		HashMap<String,String> map = new HashMap<String,String>();
		for(Tag child:children) {
			Object value = child.getValue();
			map.put(child.getName(), value==null ? "" : value.toString());
		}
		return map;
	}
	
	// Data structures used internally /////////////////////////////////////////
	
	private static class ImmutableCollection<E> implements Collection<E>,
		Serializable {

		Collection<? extends E> c;
	
		ImmutableCollection(Collection<? extends E> c) {
	        if (c==null)
	            throw new NullPointerException();
	        this.c = c;
		}
	
		public int size() { return c.size(); }
		public boolean isEmpty() { return c.isEmpty(); }
		public boolean contains(Object o) { return c.contains(o); }
		public Object[] toArray() { return c.toArray(); }
		public <T> T[] toArray(T[] a) { return c.toArray(a); }
	    public String toString() { return c.toString(); }
	
		public Iterator<E> iterator() {
		    return new Iterator<E>() {
				Iterator<? extends E> i = c.iterator();
		
				public boolean hasNext() {return i.hasNext();}
				public E next() {return i.next();}
				public void remove() {
				    throw new UnsupportedOperationException();
		        }
		    };
		}
	
		public boolean add(E o){
		    throw new UnsupportedOperationException();
		}
		
		public boolean remove(Object o) {
		    throw new UnsupportedOperationException();
		}
	
		public boolean containsAll(Collection<?> coll) {
		    return c.containsAll(coll);
		}
		
		public boolean addAll(Collection<? extends E> coll) {
		    throw new UnsupportedOperationException();
		}
		
		public boolean removeAll(Collection<?> coll) {
		    throw new UnsupportedOperationException();
		}
		
		public boolean retainAll(Collection<?> coll) {
		    throw new UnsupportedOperationException();
		}
		
		public void clear() {
		    throw new UnsupportedOperationException();
		}
	}

	private static class ImmutableSet<E> extends ImmutableCollection<E>
					 implements Set<E>, Serializable {
	
		ImmutableSet(Set<? extends E> s) { super(s); }
		public boolean equals(Object o) { return c.equals(o); }
		public int hashCode() { return c.hashCode(); }
	}	
	
	private static class ImmutableMap<K, V> implements Map<K, V>, Serializable {

		private final Map<? extends K, ? extends V> delegate;

		ImmutableMap(Map<? extends K, ? extends V> delegate) {
			if (delegate == null)
				throw new NullPointerException();
			this.delegate = delegate;
		}

		public int size() {
			return delegate.size();
		}

		public boolean isEmpty() {
			return delegate.isEmpty();
		}

		public boolean containsKey(Object key) {
			return delegate.containsKey(key);
		}

		public boolean containsValue(Object val) {
			return delegate.containsValue(val);
		}

		public V get(Object key) {
			return delegate.get(key);
		}

		public V put(K key, V value) {
			throw new UnsupportedOperationException();
		}

		public V remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public void putAll(Map<? extends K, ? extends V> t) {
			throw new UnsupportedOperationException();
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}

		private transient Set<K> keySet = null;

		private transient Set<Map.Entry<K, V>> entrySet = null;

		private transient Collection<V> values = null;

		public Set<K> keySet() {
			if (keySet == null)
				keySet = new ImmutableSet(delegate.keySet());
			return keySet;
		}

		public Set<Map.Entry<K, V>> entrySet() {
			if (entrySet == null)
				entrySet = new ImmutableEntrySet<K, V>(delegate.entrySet());
			return entrySet;
		}

		public Collection<V> values() {
			if (values == null)
				values = new ImmutableCollection(delegate.values());
			return values;
		}

		public boolean equals(Object o) {
			return delegate.equals(o);
		}

		public int hashCode() {
			return delegate.hashCode();
		}

		public String toString() {
			return delegate.toString();
		}

		static class ImmutableEntrySet<K, V> extends
				ImmutableSet<Map.Entry<K, V>> {
			private static final long serialVersionUID = 7854390611657943733L;

			ImmutableEntrySet(
					Set<? extends Map.Entry<? extends K, ? extends V>> s) {
				super((Set<Map.Entry<K, V>>) (Set) s);
			}

			public Iterator<Map.Entry<K, V>> iterator() {
				return new Iterator<Map.Entry<K, V>>() {
					Iterator<? extends Map.Entry<? extends K, ? extends V>> i = c
							.iterator();

					public boolean hasNext() {
						return i.hasNext();
					}

					public Map.Entry<K, V> next() {
						return new ImmutableEntry<K, V>(i.next());
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			public Object[] toArray() {
				Object[] a = c.toArray();
				for (int i = 0; i < a.length; i++)
					a[i] = new ImmutableEntry<K, V>((Map.Entry<K, V>) a[i]);
				return a;
			}

			public <T> T[] toArray(T[] a) {
				Object[] arr = c.toArray(a.length == 0 ? a
						: (T[]) java.lang.reflect.Array.newInstance(a
								.getClass().getComponentType(), 0));

				for (int i = 0; i < arr.length; i++)
					arr[i] = new ImmutableEntry<K, V>((Map.Entry<K, V>) arr[i]);

				if (arr.length > a.length)
					return (T[]) arr;

				System.arraycopy(arr, 0, a, 0, arr.length);
				if (a.length > arr.length)
					a[arr.length] = null;
				return a;
			}

			public boolean contains(Object o) {
				if (!(o instanceof Map.Entry))
					return false;
				return c
						.contains(new ImmutableEntry<K, V>((Map.Entry<K, V>) o));
			}

			public boolean containsAll(Collection<?> coll) {
				Iterator<?> e = coll.iterator();
				while (e.hasNext())
					if (!contains(e.next())) // Invokes safe contains() above
						return false;
				return true;
			}

			public boolean equals(Object o) {
				if (o == this)
					return true;

				if (!(o instanceof Set))
					return false;
				Set s = (Set) o;
				if (s.size() != c.size())
					return false;
				return containsAll(s); // Invokes safe containsAll() above
			}

			private static class ImmutableEntry<K, V> implements
					Map.Entry<K, V> {
				private Map.Entry<? extends K, ? extends V> e;

				ImmutableEntry(Map.Entry<? extends K, ? extends V> e) {
					this.e = e;
				}

				public K getKey() {
					return e.getKey();
				}

				public V getValue() {
					return e.getValue();
				}

				public V setValue(V value) {
					throw new UnsupportedOperationException();
				}

				public int hashCode() {
					return e.hashCode();
				}

				public boolean equals(Object o) {
					if (!(o instanceof Map.Entry))
						return false;
					Map.Entry t = (Map.Entry) o;
					return Tag.equals(e.getKey(), t.getKey())
							&& Tag.equals(e.getValue(), t.getValue());
				}

				public String toString() {
					return e.toString();
				}
			}
		}
	}
	
	private static class ImmutableSortedMap<K, V> extends ImmutableMap<K, V>
		implements SortedMap<K, V>, Serializable {

		private SortedMap<K, ? extends V> delegate;

		ImmutableSortedMap(SortedMap<K, ? extends V> m) {
			super(m);
			delegate = m;
		}
		
		public Comparator<? super K> comparator() {
			return delegate.comparator();
		}

		public SortedMap<K, V> subMap(K fromKey, K toKey) {
			return new ImmutableSortedMap<K, V>(delegate.subMap(fromKey, toKey));
		}

		public SortedMap<K, V> headMap(K toKey) {
			return new ImmutableSortedMap<K, V>(delegate.headMap(toKey));
		}

		public SortedMap<K, V> tailMap(K fromKey) {
			return new ImmutableSortedMap<K, V>(delegate.tailMap(fromKey));
		}

		public K firstKey() {
			return delegate.firstKey();
		}

		public K lastKey() {
			return delegate.lastKey();
		}
	}	
	
    private static boolean equals(Object o1, Object o2) {
        return (o1==null ? o2==null : o1.equals(o2));
    }	
}