package org.jogamp.java3d.utils.shader;

import org.jogamp.java3d.ColoringAttributes;
import org.jogamp.java3d.GLSLShaderProgram;
import org.jogamp.java3d.LineAttributes;
import org.jogamp.java3d.Material;
import org.jogamp.java3d.NodeComponent;
import org.jogamp.java3d.PointAttributes;
import org.jogamp.java3d.PolygonAttributes;
import org.jogamp.java3d.RenderingAttributes;
import org.jogamp.java3d.Shader;
import org.jogamp.java3d.ShaderAppearance;
import org.jogamp.java3d.ShaderAttributeSet;
import org.jogamp.java3d.ShaderAttributeValue;
import org.jogamp.java3d.SourceCodeShader;
import org.jogamp.java3d.TexCoordGeneration;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TextureAttributes;
import org.jogamp.java3d.TextureUnitState;
import org.jogamp.java3d.TransparencyAttributes;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Vector4f;

/**
 * @author phil
 *
 */
public class SimpleShaderAppearance extends ShaderAppearance
{
	
	public static String versionString = "#version 120\n";
	
	private static GLSLShaderProgram flatShaderProgram;
	private static GLSLShaderProgram textureShaderProgram;
	private static GLSLShaderProgram colorLineShaderProgram;
	private static GLSLShaderProgram litFlatShaderProgram;
	private static GLSLShaderProgram litTextureShaderProgram;

	public static String alphaTestUniforms = "uniform int alphaTestEnabled;\n" + //
			"uniform int alphaTestFunction;\n" + //
			"uniform float alphaTestValue;\n";

	public static String alphaTestMethod = "if(alphaTestEnabled != 0)\n" + //
			"{	\n" + //
			" 	if(alphaTestFunction==516)//>\n" + //
			"		if(baseMap.a<=alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==518)//>=\n" + //
			"		if(baseMap.a<alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==514)//==\n" + //
			"		if(baseMap.a!=alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==517)//!=\n" + //
			"		if(baseMap.a==alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==513)//<\n" + //
			"		if(baseMap.a>=alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==515)//<=\n" + //
			"		if(baseMap.a>alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==512)//never	\n" + //
			"		discard;	\n" + //
			"}\n";

	private boolean buildBasedOnAttributes = false;

	/**
	 * This will define the shader code based on the attributes set
	 */
	public SimpleShaderAppearance()
	{
		buildBasedOnAttributes = true;
		this.setCapability(ALLOW_SHADER_PROGRAM_WRITE);// as we will be re-writing a bit
		this.setCapability(ALLOW_SHADER_ATTRIBUTE_SET_WRITE);
	}

	/**
	 * Lines with a single color no texture, ignores vertex attribute of color
	 * @param color
	 */
	public SimpleShaderAppearance(Color3f color)
	{
		this(color, false, false);
	}

	/**
	 * Polygons  if hasTexture is true a texture otherwise vertex attribute colors for face color
	 */
	public SimpleShaderAppearance(boolean hasTexture)
	{
		this(null, false, hasTexture);
	}

	public SimpleShaderAppearance(boolean lit, boolean hasTexture)
	{
		this(null, lit, hasTexture);
	}

	/** if color is not null then a line appearance
	 * otherwise simple poly appearance
	 * @param color
	 */
	public SimpleShaderAppearance(Color3f color, boolean lit, boolean hasTexture)
	{
		if (lit)
		{
			String vertexProgram = versionString;
			vertexProgram += "attribute vec4 glVertex;\n";
			vertexProgram += "attribute vec4 glColor;\n";
			vertexProgram += "attribute vec3 glNormal; \n";
			if (hasTexture)
			{
				vertexProgram += "attribute vec2 glMultiTexCoord0;\n";
			}
			vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
			vertexProgram += "uniform mat4 glModelViewMatrix;\n";
			vertexProgram += "uniform mat3 glNormalMatrix;\n";
			vertexProgram += "uniform int ignoreVertexColors;\n";
			vertexProgram += "uniform vec4 glLightModelambient;\n";
			vertexProgram += "struct material\n";
			vertexProgram += "{\n";
			vertexProgram += "	int lightEnabled;\n";
			vertexProgram += " 	vec4 ambient;\n";
			vertexProgram += " 	vec4 diffuse;\n";
			vertexProgram += " 	vec4 emission; \n";
			vertexProgram += " 	vec3 specular;\n";
			vertexProgram += " 	float shininess;\n";
			vertexProgram += "};\n";
			vertexProgram += "uniform material glFrontMaterial;\n";
			vertexProgram += "struct lightSource\n";
			vertexProgram += "	{\n";
			vertexProgram += "	  vec4 position;\n";
			vertexProgram += "	  vec4 diffuse;\n";
			vertexProgram += "	  vec4 specular;\n";
			vertexProgram += "	  float constantAttenuation, linearAttenuation, quadraticAttenuation;\n";
			vertexProgram += "	  float spotCutoff, spotExponent;\n";
			vertexProgram += "	  vec3 spotDirection;\n";
			vertexProgram += "	};\n";
			vertexProgram += "\n";
			vertexProgram += "	uniform int numberOfLights;\n";
			vertexProgram += "	const int maxLights = 1;\n";
			vertexProgram += "	uniform lightSource glLightSource[maxLights];\n";
			if (hasTexture)
			{
				vertexProgram += "varying vec2 glTexCoord0;\n";
			}
			vertexProgram += "varying  vec3 LightDir;\n";
			vertexProgram += "varying  vec3 ViewDir;\n";
			vertexProgram += "varying  vec3 N;\n";
			vertexProgram += "varying  vec4 A;\n";
			vertexProgram += "varying  vec4 C;\n";
			vertexProgram += "varying  vec4 D;\n";
			vertexProgram += "varying  vec3 emissive;\n";
			vertexProgram += "varying  vec3 specular;\n";
			vertexProgram += "varying  float shininess;\n";
			vertexProgram += "void main( void ){\n";
			vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
			if (hasTexture)
			{
				vertexProgram += "glTexCoord0 = glMultiTexCoord0.st;\n";
			}

			vertexProgram += "N = normalize(glNormalMatrix * glNormal);\n";

			vertexProgram += "vec3 v = vec3(glModelViewMatrix * glVertex);\n";

			vertexProgram += "ViewDir = -v.xyz;\n";
			vertexProgram += "LightDir = glLightSource[0].position.xyz;\n";

			vertexProgram += "A = glLightModelambient * glFrontMaterial.ambient;\n";
			vertexProgram += "if( ignoreVertexColors != 0) \n";
			// objectColor should be used if it is no lighting, and reusing material diffuse appears wrong
			vertexProgram += "	C = vec4(1,1,1,1);//glFrontMaterial.diffuse; \n";
			vertexProgram += "else \n";
			vertexProgram += "	C = glColor; \n";

			vertexProgram += "D = glLightSource[0].diffuse * glFrontMaterial.diffuse;\n";

			vertexProgram += "emissive = glFrontMaterial.emission.rgb;\n";
			vertexProgram += "specular = glFrontMaterial.specular;\n";
			vertexProgram += "shininess = glFrontMaterial.shininess;\n";
			vertexProgram += "}";

			String fragmentProgram = versionString;
			fragmentProgram += "precision mediump float;\n";
			if (hasTexture)
			{
				fragmentProgram += alphaTestUniforms;

				fragmentProgram += "varying vec2 glTexCoord0;\n";
				fragmentProgram += "uniform sampler2D BaseMap;\n";
			}

			fragmentProgram += "in vec3 LightDir;\n";
			fragmentProgram += "in vec3 ViewDir;\n";

			fragmentProgram += "in vec3 N;\n";

			fragmentProgram += "in vec4 A;\n";
			fragmentProgram += "in vec4 C;\n";
			fragmentProgram += "in vec4 D;\n";

			fragmentProgram += "in vec3 emissive;\n";
			fragmentProgram += "in vec3 specular;\n";
			fragmentProgram += "in float shininess;\n";
			fragmentProgram += "void main( void ){\n ";
			if (hasTexture)
			{
				fragmentProgram += "vec4 baseMap = texture2D( BaseMap, glTexCoord0.st );\n";
			}
			if (hasTexture)
			{
				fragmentProgram += alphaTestMethod;
			}
			fragmentProgram += "vec3 normal = N;\n";

			fragmentProgram += "vec3 L = normalize(LightDir);\n";
			fragmentProgram += "vec3 E = normalize(ViewDir);\n";
			fragmentProgram += "vec3 R = reflect(-L, normal);\n";
			fragmentProgram += "vec3 H = normalize( L + E );\n";

			fragmentProgram += "float NdotL = max( dot(normal, L), 0.0 );\n";
			fragmentProgram += "float NdotH = max( dot(normal, H), 0.0 );\n";
			fragmentProgram += "float EdotN = max( dot(normal, E), 0.0 );\n";
			fragmentProgram += "float NdotNegL = max( dot(normal, -L), 0.0 );\n";

			fragmentProgram += "vec4 color;\n";
			if (hasTexture)
			{
				fragmentProgram += "vec3 albedo = baseMap.rgb * C.rgb;\n";
			}
			else
			{
				fragmentProgram += "vec3 albedo = C.rgb;\n";
			}
			fragmentProgram += "vec3 diffuse = A.rgb + (D.rgb * NdotL);\n";

			// 0.3 is just what the calc is
			fragmentProgram += "vec3 spec = specular * pow(NdotH, 0.3*shininess);\n";
			// D is not right it should be the light source spec color, probably just 1,1,1 but java3d has no spec on lights
			//fragmentProgram += "spec *= D.rgb;\n";

			fragmentProgram += "color.rgb = albedo * (diffuse + emissive) + spec;\n";
			if (hasTexture)
			{
				fragmentProgram += "color.a = C.a * baseMap.a;\n";
			}
			else
			{
				fragmentProgram += "color.a = C.a;\n";
			}

			fragmentProgram += "gl_FragColor = color;\n";

			fragmentProgram += "}";
			if (hasTexture)
			{
				if (litTextureShaderProgram == null)
				{
					litTextureShaderProgram = new GLSLShaderProgram() {
						@Override
						public String toString()
						{
							return "SimpleShaderAppearance litTextureShaderProgram";
						}
					};
					litTextureShaderProgram.setShaders(makeShaders(vertexProgram, fragmentProgram));
					litTextureShaderProgram.setShaderAttrNames(new String[] { "BaseMap" });

				}

				setShaderProgram(litTextureShaderProgram);

				ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();
				shaderAttributeSet.put(new ShaderAttributeValue("BaseMap", new Integer(0)));
				setShaderAttributeSet(shaderAttributeSet);
			}
			else
			{
				if (litFlatShaderProgram == null)
				{
					litFlatShaderProgram = new GLSLShaderProgram() {
						@Override
						public String toString()
						{
							return "SimpleShaderAppearance litFlatShaderProgram";
						}
					};
					litFlatShaderProgram.setShaders(makeShaders(vertexProgram, fragmentProgram));

					//System.out.println("vertexProgram " + vertexProgram);
					//System.out.println("fragmentProgram " + fragmentProgram);

				}

				setShaderProgram(litFlatShaderProgram);

			}
		}
		else
		{
			if (hasTexture)
			{
				if (textureShaderProgram == null)
				{
					textureShaderProgram = new GLSLShaderProgram() {
						@Override
						public String toString()
						{
							return "SimpleShaderAppearance textureShaderProgram";
						}
					};
					String vertexProgram = versionString;
					vertexProgram += "attribute vec4 glVertex;\n";
					vertexProgram += "attribute vec2 glMultiTexCoord0;\n";
					vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
					vertexProgram += "varying vec2 glTexCoord0;\n";
					vertexProgram += "void main( void ){\n";
					vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
					vertexProgram += "glTexCoord0 = glMultiTexCoord0.st;\n";
					vertexProgram += "}";

					String fragmentProgram = versionString;
					fragmentProgram += "precision mediump float;\n";
					fragmentProgram += alphaTestUniforms;
					fragmentProgram += "varying vec2 glTexCoord0;\n";
					fragmentProgram += "uniform sampler2D BaseMap;\n";
					fragmentProgram += "void main( void ){\n ";
					fragmentProgram += "vec4 baseMap = texture2D( BaseMap, glTexCoord0.st );\n";
					fragmentProgram += alphaTestMethod;
					fragmentProgram += "gl_FragColor = baseMap;\n";
					fragmentProgram += "}";

					textureShaderProgram.setShaders(makeShaders(vertexProgram, fragmentProgram));
					textureShaderProgram.setShaderAttrNames(new String[] { "BaseMap" });
				}

				setShaderProgram(textureShaderProgram);

				ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();
				shaderAttributeSet.put(new ShaderAttributeValue("BaseMap", new Integer(0)));
				setShaderAttributeSet(shaderAttributeSet);

			}
			else
			{
				if (color != null)
				{
					PolygonAttributes polyAtt = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0.0f);
					polyAtt.setPolygonOffset(0.1f);
					setPolygonAttributes(polyAtt);
					LineAttributes lineAtt = new LineAttributes(1, LineAttributes.PATTERN_SOLID, false);
					setLineAttributes(lineAtt);

					ColoringAttributes colorAtt = new ColoringAttributes(color, ColoringAttributes.FASTEST);
					setColoringAttributes(colorAtt);

					RenderingAttributes ra = new RenderingAttributes();
					ra.setIgnoreVertexColors(true);
					setRenderingAttributes(ra);

					Material mat = new Material();
					setMaterial(mat);

					if (colorLineShaderProgram == null)
					{
						colorLineShaderProgram = new GLSLShaderProgram() {
							@Override
							public String toString()
							{
								return "SimpleShaderAppearance colorLineShaderProgram";
							}
						};
						String vertexProgram = versionString;
						vertexProgram += "attribute vec4 glVertex;\n";
						vertexProgram += "attribute vec4 glColor;\n";
						vertexProgram += "uniform int ignoreVertexColors;\n";
						vertexProgram += "uniform vec4 objectColor;\n";
						vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
						vertexProgram += "varying vec4 glFrontColor;\n";
						vertexProgram += "void main( void ){\n";
						vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
						vertexProgram += "if( ignoreVertexColors != 0 )\n";
						vertexProgram += "	glFrontColor = objectColor;\n";
						vertexProgram += "else\n";
						vertexProgram += "	glFrontColor = glColor;\n";
						vertexProgram += "}";

						String fragmentProgram = versionString;
						fragmentProgram += "precision mediump float;\n";
						fragmentProgram += "varying vec4 glFrontColor;\n";
						fragmentProgram += "void main( void ){\n";
						fragmentProgram += "gl_FragColor = glFrontColor;\n";
						fragmentProgram += "}";

						colorLineShaderProgram.setShaders(makeShaders(vertexProgram, fragmentProgram));
					}

					setShaderProgram(colorLineShaderProgram);

				}
				else
				{

					if (flatShaderProgram == null)
					{
						flatShaderProgram = new GLSLShaderProgram() {
							@Override
							public String toString()
							{
								return "SimpleShaderAppearance flatShaderProgram";
							}
						};
						String vertexProgram = versionString;
						vertexProgram += "attribute vec4 glVertex;\n";
						vertexProgram += "attribute vec4 glColor;\n";
						vertexProgram += "uniform int ignoreVertexColors;\n";
						vertexProgram += "uniform vec4 objectColor;\n";
						vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
						vertexProgram += "varying vec4 glFrontColor;\n";
						vertexProgram += "void main( void ){\n";
						vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
						vertexProgram += "if( ignoreVertexColors != 0 )\n";
						vertexProgram += "	glFrontColor = objectColor;\n";
						vertexProgram += "else\n";
						vertexProgram += "	glFrontColor = glColor;\n";
						vertexProgram += "}";

						String fragmentProgram = versionString;
						fragmentProgram += "precision mediump float;\n";
						fragmentProgram += "varying vec4 glFrontColor;\n";
						fragmentProgram += "void main( void ){\n";
						fragmentProgram += "gl_FragColor = glFrontColor;\n";
						fragmentProgram += "}";

						flatShaderProgram.setShaders(makeShaders(vertexProgram, fragmentProgram));

					}

					setShaderProgram(flatShaderProgram);

				}
			}

		}

	}

	private static Shader[] makeShaders(String vertexProgram, String fragmentProgram)
	{
		Shader[] shaders = new Shader[2];
		shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_VERTEX, vertexProgram) {
			@Override
			public String toString()
			{
				return "vertexProgram";
			}
		};
		shaders[1] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_FRAGMENT, fragmentProgram) {
			@Override
			public String toString()
			{
				return "fragmentProgram";
			}
		};
		return shaders;
	}

	private static GLSLShaderProgram flatShaderProgram2;
	private static GLSLShaderProgram textureShaderProgram2;
	private static GLSLShaderProgram colorLineShaderProgram2;
	private static GLSLShaderProgram litFlatShaderProgram2;
	private static GLSLShaderProgram litTextureShaderProgram2;

	// we can't set it in the super calss as tex coord gen is not supported
	private TexCoordGeneration texCoordGeneration = null;

	private void rebuildShaders()
	{
		if (buildBasedOnAttributes)
		{
			boolean hasTexture = this.getTexture() != null || this.getTextureUnitCount() > 0;
			if(this.getTextureUnitCount()>0)
				System.out.println("this.getTextureUnitCount() "+this.getTextureUnitCount());
			boolean lit = this.getMaterial() != null; // having material== lit geometry

			boolean hasTextureCoordGen = hasTexture && texCoordGeneration != null;

			if (hasTextureCoordGen)
			{
				if (texCoordGeneration.getFormat() != 0)
					System.out.println("texCoordGeneration.getFormat() must be 0");
				/** 
				 * Generates texture coordinates as a linear function in object coordinates.
				 public static final int OBJECT_LINEAR = 0;				    
				 * Generates texture coordinates as a linear function in eye coordinates.				    
				 public static final int EYE_LINEAR    = 1;				   
				 * Generates texture coordinates using a spherical reflection mapping in eye coordinates.
				 public static final int SPHERE_MAP    = 2;
				 * Generates texture coordinates that match vertices' normals in eye coordinates.
				 public static final int NORMAL_MAP    = 3;
				 * Generates texture coordinates that match vertices' reflection vectors in eye coordinates.
				 public static final int REFLECTION_MAP = 4;
				 see multitex.vert in examples for sphere map and cube map and google for the others
				 */
			}

			if (lit)
			{
				String vertexProgram = versionString;
				vertexProgram += "attribute vec4 glVertex;\n";
				vertexProgram += "attribute vec4 glColor;\n";
				vertexProgram += "attribute vec3 glNormal; \n";
				if (hasTexture && !hasTextureCoordGen)
				{
					vertexProgram += "attribute vec2 glMultiTexCoord0;\n";
				}
				vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
				vertexProgram += "uniform mat4 glModelViewMatrix;\n";
				vertexProgram += "uniform mat3 glNormalMatrix;\n";
				vertexProgram += "uniform int ignoreVertexColors;\n";
				vertexProgram += "uniform vec4 glLightModelambient;\n";
				vertexProgram += "struct material\n";
				vertexProgram += "{\n";
				vertexProgram += "	int lightEnabled;\n";
				vertexProgram += " 	vec4 ambient;\n";
				vertexProgram += " 	vec4 diffuse;\n";
				vertexProgram += " 	vec4 emission; \n";
				vertexProgram += " 	vec3 specular;\n";
				vertexProgram += " 	float shininess;\n";
				vertexProgram += "};\n";
				vertexProgram += "uniform material glFrontMaterial;\n";
				vertexProgram += "struct lightSource\n";
				vertexProgram += "	{\n";
				vertexProgram += "	  vec4 position;\n";
				vertexProgram += "	  vec4 diffuse;\n";
				vertexProgram += "	  vec4 specular;\n";
				vertexProgram += "	  float constantAttenuation, linearAttenuation, quadraticAttenuation;\n";
				vertexProgram += "	  float spotCutoff, spotExponent;\n";
				vertexProgram += "	  vec3 spotDirection;\n";
				vertexProgram += "	};\n";
				vertexProgram += "\n";
				vertexProgram += "	uniform int numberOfLights;\n";
				vertexProgram += "	const int maxLights = 1;\n";
				vertexProgram += "	uniform lightSource glLightSource[maxLights];\n";
				if (hasTextureCoordGen && texCoordGeneration.getGenMode() == TexCoordGeneration.OBJECT_LINEAR)
				{
					vertexProgram += "uniform vec4 texCoordGenPlaneS;\n";
					vertexProgram += "uniform vec4 texCoordGenPlaneT;\n";
				}
				if (hasTexture)
				{
					vertexProgram += "varying vec2 glTexCoord0;\n";
				}
				vertexProgram += "varying  vec3 LightDir;\n";
				vertexProgram += "varying  vec3 ViewDir;\n";
				vertexProgram += "varying  vec3 N;\n";
				vertexProgram += "varying  vec4 A;\n";
				vertexProgram += "varying  vec4 C;\n";
				vertexProgram += "varying  vec4 D;\n";
				vertexProgram += "varying  vec3 emissive;\n";
				vertexProgram += "varying  vec3 specular;\n";
				vertexProgram += "varying  float shininess;\n";
				if (hasTextureCoordGen)
				{
					vertexProgram += "vec2 object_linear(vec4 pos, vec4 planeOS, vec4 planeOT)\n";
					vertexProgram += "{\n";
					vertexProgram += "	return vec2(pos.x*planeOS.x+pos.y*planeOS.y+pos.z*planeOS.z+pos.w*planeOS.w,pos.x*planeOT.x+pos.y*planeOT.y+pos.z*planeOT.z+pos.w*planeOT.w);\n";
					vertexProgram += "}\n";
				}

				vertexProgram += "void main( void ){\n";
				vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
				vertexProgram += "N = normalize(glNormalMatrix * glNormal);\n";
				if (hasTexture)
				{
					if (!hasTextureCoordGen)
					{
						vertexProgram += "glTexCoord0 = glMultiTexCoord0.st;\n";
					}
					else
					{
						if (texCoordGeneration.getGenMode() == TexCoordGeneration.OBJECT_LINEAR)
						{ 
							vertexProgram += "glTexCoord0 = object_linear(glVertex, texCoordGenPlaneS, texCoordGenPlaneT);\n"; 
						}
						else
						{
							System.err.println("texCoordGeneration.getGenMode() not supported " + texCoordGeneration.getGenMode());
						}
					}
				}

				vertexProgram += "vec3 v = vec3(glModelViewMatrix * glVertex);\n";

				vertexProgram += "ViewDir = -v.xyz;\n";
				vertexProgram += "LightDir = glLightSource[0].position.xyz;\n";

				vertexProgram += "A = glLightModelambient * glFrontMaterial.ambient;\n";
				vertexProgram += "if( ignoreVertexColors != 0) \n";
				// objectColor should be used if it is no lighting, and reusing material diffuse appears wrong
				vertexProgram += "	C = vec4(1,1,1,1);//glFrontMaterial.diffuse; \n";
				vertexProgram += "else \n";
				vertexProgram += "	C = glColor; \n";

				vertexProgram += "D = glLightSource[0].diffuse * glFrontMaterial.diffuse;\n";

				vertexProgram += "emissive = glFrontMaterial.emission.rgb;\n";
				vertexProgram += "specular = glFrontMaterial.specular;\n";
				vertexProgram += "shininess = glFrontMaterial.shininess;\n";
				vertexProgram += "}";

				String fragmentProgram = versionString;
				fragmentProgram += "precision mediump float;\n";
				if (hasTexture)
				{
					fragmentProgram += alphaTestUniforms;

					fragmentProgram += "varying vec2 glTexCoord0;\n";
					fragmentProgram += "uniform sampler2D BaseMap;\n";
				}

				fragmentProgram += "in vec3 LightDir;\n";
				fragmentProgram += "in vec3 ViewDir;\n";

				fragmentProgram += "in vec3 N;\n";

				fragmentProgram += "in vec4 A;\n";
				fragmentProgram += "in vec4 C;\n";
				fragmentProgram += "in vec4 D;\n";

				fragmentProgram += "in vec3 emissive;\n";
				fragmentProgram += "in vec3 specular;\n";
				fragmentProgram += "in float shininess;\n";
				fragmentProgram += "void main( void ){\n ";
				if (hasTexture)
				{
					fragmentProgram += "vec4 baseMap = texture2D( BaseMap, glTexCoord0.st );\n";
				}
				if (hasTexture)
				{
					fragmentProgram += alphaTestMethod;
				}
				fragmentProgram += "vec3 normal = N;\n";

				fragmentProgram += "vec3 L = normalize(LightDir);\n";
				fragmentProgram += "vec3 E = normalize(ViewDir);\n";
				fragmentProgram += "vec3 R = reflect(-L, normal);\n";
				fragmentProgram += "vec3 H = normalize( L + E );\n";

				fragmentProgram += "float NdotL = max( dot(normal, L), 0.0 );\n";
				fragmentProgram += "float NdotH = max( dot(normal, H), 0.0 );\n";
				fragmentProgram += "float EdotN = max( dot(normal, E), 0.0 );\n";
				fragmentProgram += "float NdotNegL = max( dot(normal, -L), 0.0 );\n";

				fragmentProgram += "vec4 color;\n";
				if (hasTexture)
				{
					fragmentProgram += "vec3 albedo = baseMap.rgb * C.rgb;\n";
				}
				else
				{
					fragmentProgram += "vec3 albedo = C.rgb;\n";
				}
				fragmentProgram += "vec3 diffuse = A.rgb + (D.rgb * NdotL);\n";

				// 0.3 is just what the calc is
				fragmentProgram += "vec3 spec = specular * pow(NdotH, 0.3*shininess);\n";
				// D is not right it should be the light source spec color, probably just 1,1,1 but java3d has no spec on lights
				//fragmentProgram += "spec *= D.rgb;\n";

				fragmentProgram += "color.rgb = albedo * (diffuse + emissive) + spec;\n";
				if (hasTexture)
				{
					fragmentProgram += "color.a = C.a * baseMap.a;\n";
				}
				else
				{
					fragmentProgram += "color.a = C.a;\n";
				}

				fragmentProgram += "gl_FragColor = color;\n";
				//for debug of the incorrect looking tex coord gen values
				//if (hasTexture)
				//fragmentProgram += "gl_FragColor = vec4(mod(glTexCoord0.s,1.0),mod(glTexCoord0.t,1.0),0,1);\n";

				fragmentProgram += "}";
				if (hasTexture)
				{
					// don't cache due to many variables (eg has texgen)
					//if (litTextureShaderProgram2 == null)
					{
						litTextureShaderProgram2 = new GLSLShaderProgram() {
							@Override
							public String toString()
							{
								return "SimpleShaderAppearance litTextureShaderProgram2";
							}
						};
						litTextureShaderProgram2.setShaders(makeShaders(vertexProgram, fragmentProgram));
						if (hasTextureCoordGen && texCoordGeneration.getGenMode() == TexCoordGeneration.OBJECT_LINEAR)
						{
							litTextureShaderProgram2
									.setShaderAttrNames(new String[] { "BaseMap", "texCoordGenPlaneS", "texCoordGenPlaneT" });
						}
						else
						{
							litTextureShaderProgram2.setShaderAttrNames(new String[] { "BaseMap" });
						}

					}

					setShaderProgram(litTextureShaderProgram2);

					ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();
					shaderAttributeSet.put(new ShaderAttributeValue("BaseMap", new Integer(0)));
					if (hasTextureCoordGen && texCoordGeneration.getGenMode() == TexCoordGeneration.OBJECT_LINEAR)
					{
						Vector4f planeS = new Vector4f();
						texCoordGeneration.getPlaneS(planeS);
						Vector4f planeT = new Vector4f();
						texCoordGeneration.getPlaneT(planeT);
						shaderAttributeSet.put(new ShaderAttributeValue("texCoordGenPlaneS", planeS));
						shaderAttributeSet.put(new ShaderAttributeValue("texCoordGenPlaneT", planeT));
					}
					setShaderAttributeSet(shaderAttributeSet);
				}
				else
				{
					// don't cache due to many variables (eg has texgen)
					//if (litFlatShaderProgram2 == null)
					{
						litFlatShaderProgram2 = new GLSLShaderProgram() {
							@Override
							public String toString()
							{
								return "SimpleShaderAppearance litFlatShaderProgram2";
							}
						};
						litFlatShaderProgram2.setShaders(makeShaders(vertexProgram, fragmentProgram));

						//System.out.println("vertexProgram " + vertexProgram);
						//System.out.println("fragmentProgram " + fragmentProgram);
						
						

					}

					setShaderProgram(litFlatShaderProgram2);

				}
			}
			else
			{
				if (hasTexture)
				{
					if (textureShaderProgram2 == null)
					{
						textureShaderProgram2 = new GLSLShaderProgram() {
							@Override
							public String toString()
							{
								return "SimpleShaderAppearance textureShaderProgram2";
							}
						};
						String vertexProgram = versionString;
						vertexProgram += "attribute vec4 glVertex;\n";
						vertexProgram += "attribute vec2 glMultiTexCoord0;\n";
						vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
						vertexProgram += "varying vec2 glTexCoord0;\n";
						vertexProgram += "void main( void ){\n";
						vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
						vertexProgram += "glTexCoord0 = glMultiTexCoord0.st;\n";
						vertexProgram += "}";

						String fragmentProgram = versionString;
						fragmentProgram += "precision mediump float;\n";
						fragmentProgram += alphaTestUniforms;
						fragmentProgram += "varying vec2 glTexCoord0;\n";
						fragmentProgram += "uniform sampler2D BaseMap;\n";
						fragmentProgram += "void main( void ){\n ";
						fragmentProgram += "vec4 baseMap = texture2D( BaseMap, glTexCoord0.st );\n";
						fragmentProgram += alphaTestMethod;
						fragmentProgram += "gl_FragColor = baseMap;\n";
						fragmentProgram += "}";

						textureShaderProgram2.setShaders(makeShaders(vertexProgram, fragmentProgram));
						textureShaderProgram2.setShaderAttrNames(new String[] { "BaseMap" });
					}

					setShaderProgram(textureShaderProgram2);

					ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();
					shaderAttributeSet.put(new ShaderAttributeValue("BaseMap", new Integer(0)));
					setShaderAttributeSet(shaderAttributeSet);

				}
				else
				{
					if (flatShaderProgram2 == null)
					{
						flatShaderProgram2 = new GLSLShaderProgram() {
							@Override
							public String toString()
							{
								return "SimpleShaderAppearance flatShaderProgram2";
							}
						};
						String vertexProgram = versionString;
						vertexProgram += "attribute vec4 glVertex;\n";
						vertexProgram += "attribute vec4 glColor;\n";
						vertexProgram += "uniform int ignoreVertexColors;\n";
						vertexProgram += "uniform vec4 objectColor;\n";
						vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
						vertexProgram += "varying vec4 glFrontColor;\n";
						vertexProgram += "void main( void ){\n";
						vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
						vertexProgram += "if( ignoreVertexColors != 0 )\n";
						vertexProgram += "	glFrontColor = objectColor;\n";
						vertexProgram += "else\n";
						vertexProgram += "	glFrontColor = glColor;\n";
						vertexProgram += "}";

						String fragmentProgram = versionString;
						fragmentProgram += "precision mediump float;\n";
						fragmentProgram += "varying vec4 glFrontColor;\n";
						fragmentProgram += "void main( void ){\n";
						fragmentProgram += "gl_FragColor = glFrontColor;\n";
						fragmentProgram += "}";

						flatShaderProgram2.setShaders(makeShaders(vertexProgram, fragmentProgram));

					}

					setShaderProgram(flatShaderProgram2);

				}

			}
		}
	}

	@Override
	public void setMaterial(Material material)
	{
		super.setMaterial(material);
		rebuildShaders();
	}

	@Override
	public void setColoringAttributes(ColoringAttributes coloringAttributes)
	{
		super.setColoringAttributes(coloringAttributes);
		rebuildShaders();
	}

	@Override
	public void setTransparencyAttributes(TransparencyAttributes transparencyAttributes)
	{
		super.setTransparencyAttributes(transparencyAttributes);
		rebuildShaders();
	}

	@Override
	public void setRenderingAttributes(RenderingAttributes renderingAttributes)
	{
		super.setRenderingAttributes(renderingAttributes);
		rebuildShaders();
	}

	@Override
	public void setPolygonAttributes(PolygonAttributes polygonAttributes)
	{
		super.setPolygonAttributes(polygonAttributes);
		rebuildShaders();
	}

	@Override
	public void setLineAttributes(LineAttributes lineAttributes)
	{
		super.setLineAttributes(lineAttributes);
		rebuildShaders();
	}

	@Override
	public void setPointAttributes(PointAttributes pointAttributes)
	{
		super.setPointAttributes(pointAttributes);
		rebuildShaders();
	}

	@Override
	public void setTexture(Texture texture)
	{
		super.setTexture(texture);
		rebuildShaders();
	}

	@Override
	public void setTextureAttributes(TextureAttributes textureAttributes)
	{
		super.setTextureAttributes(textureAttributes);
		rebuildShaders();
	}

	@Override
	public void setTexCoordGeneration(TexCoordGeneration texCoordGeneration)
	{
		// can't use the super version as it's not supported	
		//super.setTexCoordGeneration(texCoordGeneration);
		this.texCoordGeneration = texCoordGeneration;
		rebuildShaders();
	}

	@Override
	public void setTextureUnitState(TextureUnitState[] stateArray)
	{
		System.out.println("SimpleShaderAppearance with textureunitstates in use");
		super.setTextureUnitState(stateArray);
		rebuildShaders();
	}

	@Override
	public void setTextureUnitState(int index, TextureUnitState state)
	{
		System.out.println("SimpleShaderAppearance with textureunitstates in use");
		super.setTextureUnitState(index, state);
		rebuildShaders();
	}

	/**
	* Must implement or clones turn out to be ShaderAppearance
	*/
	@Override
	public NodeComponent cloneNodeComponent()
	{
		SimpleShaderAppearance a = new SimpleShaderAppearance();
		a.duplicateNodeComponent(this);
		return a;
	}
}
