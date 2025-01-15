/*

++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
README:
Press 1, 2 or 3 to move individual fingers of the claw
Press E to move light manually
press R to toggle constant rotation of light
Press W,D to move light up and down on the Y axis
Press A,S to move light on the X axis
Press L to toggle Fill and Line
Press Up, Down, Left, Right arrows to move camera view 


*/

package code;

import java.io.*;
import java.lang.Math;                              
import java.nio.*;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import org.joml.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Code extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private int renderingProgram1, renderingProgram2, renderingProgramCubeMap;
	private int vao[] = new int[1];
	private int vbo[] = new int[40];
   private float cameraX, cameraY, cameraZ;
	private float objLocX, objLocY, objLocZ;

	// model stuff
   private ImportedModel floor, palm, fin1_1, fin1_2, fin1_3, 
                                      fin2_1, fin2_2, fin2_3,
                                      thm1_1, thm1_2, thm1_3;
	private int numPyramidVertices,numObjVertices; 
	
	// location of floor, light, camera and other objects   
   private Vector3f initialLoc = new Vector3f(0.0f, 0.5f, 0.0f);
	private Vector3f cameraLoc = new Vector3f(0.0f, 0.2f, 5.0f);
   private Vector3f lightLoc = new Vector3f(-0.8f, 6.0f, -2.1f);
   private Vector3f floorLoc = new Vector3f(0.0f, -1.0f, 0.0f);
	
	// white light properties
	private float[] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	private float[] lightAmbient = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
	private float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	private float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	// gold material
	private float[] GmatAmb = Utils.goldAmbient();
	private float[] GmatDif = Utils.goldDiffuse();
	private float[] GmatSpe = Utils.goldSpecular();
	private float GmatShi = Utils.goldShininess();
	
	// bronze material
	private float[] BmatAmb = Utils.bronzeAmbient();
	private float[] BmatDif = Utils.bronzeDiffuse();
	private float[] BmatSpe = Utils.bronzeSpecular();
	private float BmatShi = Utils.bronzeShininess();
	
	private float[] thisAmb, thisDif, thisSpe, matAmb, matDif, matSpe;
	private float thisShi, matShi;
	
	// shadow stuff
	private int scSizeX, scSizeY;
	private int [] shadowTex = new int[1];
	private int [] shadowBuffer = new int[1];
	private Matrix4f lightVmat = new Matrix4f();
	private Matrix4f lightPmat = new Matrix4f();
	private Matrix4f shadowMVP1 = new Matrix4f();
	private Matrix4f shadowMVP2 = new Matrix4f();
	private Matrix4f b = new Matrix4f();

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int mLoc, vLoc, pLoc, nLoc, sLoc, mvLoc;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private float aspect;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];
	private Vector3f origin = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
   
   private int skyboxTexture;
   private float rotX, rotY;
   private float rotLightY = 1.0f;
   private float incR = 0.1f;
   private boolean chngFill, rotateSun;
   private boolean movFin1, movFin2, movThum = false;
   private double tf, startTime, elapsedTime;
   

	public Code()
	{	setTitle("Final Project-Team Caribbean-Code 1");
		setSize(800, 800);
      
      myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		myCanvas.addKeyListener(this);
      myCanvas.setFocusable(true);
		this.add(myCanvas);
		this.setVisible(true);
      
      //add animation
		Animator animator = new Animator(myCanvas);
		animator.start();
	}
   
   public void display(GLAutoDrawable drawable)
   {  GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
    
      elapsedTime = System.currentTimeMillis() - startTime;
      tf = elapsedTime/1000.0;  // time factor
    
      // draw cube map
		gl.glUseProgram(renderingProgramCubeMap);

		vLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "v_matrix");
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));

		pLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "p_matrix");
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
				
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[39]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);	     // cube is CW, but we are viewing the inside
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);
      //end cubemap
    
      currentLightPos.set(lightLoc);
      currentLightPos.rotateY((float)rotLightY);
    

      gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer[0]);
      gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex[0], 0);
      gl.glClear(GL_DEPTH_BUFFER_BIT);
    
      gl.glDrawBuffer(GL_NONE);
      gl.glEnable(GL_DEPTH_TEST);
      gl.glEnable(GL_POLYGON_OFFSET_FILL);
      gl.glPolygonOffset(3.0f, 5.0f);
    
      passOne();
    
      gl.glDisable(GL_POLYGON_OFFSET_FILL);
    
      gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
      gl.glActiveTexture(GL_TEXTURE0);
      gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);

      gl.glDrawBuffer(GL_FRONT);
    
      //change from fill to line
      if (chngFill) {
         gl.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
      } else { 
         gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL); 
      }
      
      if (rotateSun) {
         rotLightY += 0.01f;
      } else { 
         ; 
      }
    
    passTwo();
}

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passOne()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
      gl.glUseProgram(renderingProgram1);
      
      drawObjPass1(gl, floor, 0, floorLoc);
      drawObjPass1(gl, palm, 1, initialLoc);
      drawObjPass1(gl, fin1_1, 2, initialLoc);
      drawObjPass1(gl, fin1_2, 3, initialLoc);
      drawObjPass1(gl, fin1_3, 4, initialLoc);
      drawObjPass1(gl, fin2_1, 5, initialLoc);
      drawObjPass1(gl, fin2_2, 6, initialLoc);
      drawObjPass1(gl, fin2_3, 7, initialLoc);
      drawObjPass1(gl, thm1_1, 8, initialLoc);
      drawObjPass1(gl, thm1_2, 9, initialLoc);
      drawObjPass1(gl, thm1_3, 10, initialLoc);
	}
   
   public void drawObjPass1(GL4 gl,ImportedModel myModel, int modelIndex, Vector3f loc) { 

      sLoc = gl.glGetUniformLocation(renderingProgram1, "shadowMVP");
      
      //draw objects in their location
      mMat.identity();
		mMat.translate(loc.x(), loc.y(), loc.z());
      
      if (movFin1){       //finger 1 movement
        mMat.identity();
         if (modelIndex==2) {
         //individual movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
         }
         if (modelIndex==3) {
        //inherit movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
         //individual movement 2
        mMat.translate(0.0f, 0.638079f, -0.738314f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.638079f, 0.738314f);
         }
         if (modelIndex==4) {
         //inherit movement 2
        mMat.translate(0.0f, 0.638079f, -0.738314f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.638079f, 0.738314f);
        //inherit movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
        //individaul movement 3
        mMat.translate(0.0f, 0.45f, -1.2f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.45f, 1.2f); 
        }
      
      }if (movFin2){       //finger 2 movement
         if (modelIndex==5) {
        //individual movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);   
         }
         if (modelIndex==6) {
        //inherit movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
        //individual movement 2
        mMat.translate(0.0f, 0.638079f, -0.738314f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.638079f, 0.738314f);
        }
        if (modelIndex==7) {
        //inherit movement 2
        mMat.translate(0.0f, 0.638079f, -0.738314f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.638079f, 0.738314f);
        //inherit movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
        //individaul movement 3
        mMat.translate(0.0f, 0.45f, -1.2f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.45f, 1.2f);           }     
      
   } if (movThum) {   //thumb movement
         if (modelIndex==8) {
        //individual momement 1
        mMat.translate(-0.613613f, 0.0f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.613613f, 0.0f, 0.0f); 
        }
         if (modelIndex==9) {
        //inherit movement 1
        mMat.translate(-0.613613f, 0.0f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.613613f, 0.0f, 0.0f); 
        //individual movement 2
        mMat.translate(-0.821455f, -0.35f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.821455f, 0.35f, 0.0f);    
        }
        if (modelIndex==10) {
        //inherit movement 2
        mMat.translate(-0.821455f, -0.399856f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.821455f, 0.399856f, 0.0f); 
        //individual movement 3
        mMat.translate(-0.803631f, -0.574026f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.803631f, 0.574026f, 0.0f); 
        }
      }
      
      if (modelIndex == 0){ //floor
        mMat.identity();
        mMat.translate(loc.x(), loc.y(), loc.z());  // Apply floor location
        mMat.scale(3.0f, 3.0f, 3.0f);  // Scale after translation
		}
      
		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
      
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
		
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0 + modelIndex*3]);
 		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
	   gl.glEnableVertexAttribArray(0);	
      
      gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());
	   

   }
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passTwo()
	{	
      GL4 gl = (GL4) GLContext.getCurrentGL();
      
      drawObjPass2(gl, floor, 0, floorLoc);
      drawObjPass2(gl, palm, 1, initialLoc);
      drawObjPass2(gl, fin1_1, 2, initialLoc);
      drawObjPass2(gl, fin1_2, 3, initialLoc);
      drawObjPass2(gl, fin1_3, 4, initialLoc);
      drawObjPass2(gl, fin2_1, 5, initialLoc);
      drawObjPass2(gl, fin2_2, 6, initialLoc);
      drawObjPass2(gl, fin2_3, 7, initialLoc);
      drawObjPass2(gl, thm1_1, 8, initialLoc);
      drawObjPass2(gl, thm1_2, 9, initialLoc);
      drawObjPass2(gl, thm1_3, 10, initialLoc);
	}
   
   public void drawObjPass2(GL4 gl,ImportedModel myModel, int modelIndex, Vector3f loc) {
   
		gl.glUseProgram(renderingProgram2);
		
		mLoc = gl.glGetUniformLocation(renderingProgram2, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram2, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram2, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram2, "norm_matrix");
		sLoc = gl.glGetUniformLocation(renderingProgram2, "shadowMVP");
      
      //color
      if(modelIndex==0){
       //bronze for floor
   		thisAmb = BmatAmb; 
   		thisDif = BmatDif;
   		thisSpe = BmatSpe;
   		thisShi = BmatShi;
        
      }else {
         //gold for hand
         thisAmb = GmatAmb; 
   		thisDif = GmatDif;
   		thisSpe = GmatSpe;
   		thisShi = GmatShi;
      }
		
      vMat.identity().setTranslation(-cameraLoc.x(), -cameraLoc.y(), -cameraLoc.z());
      vMat.rotateX(rotX);  //used vMat.rotate to rotate the camera instead of the model
      vMat.rotateY(rotY);  //used vMat.rotate to rotate the camera instead of the model

		installLights(renderingProgram2);
		
      mMat.identity();
		mMat.translate(loc.x(), loc.y(), loc.z());
      
      mMat.identity(); //important
      
      if (movFin1){       //finger 1 movement
        mMat.identity();
         if (modelIndex==2) {
         //individual movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
         }
         if (modelIndex==3) {
        //inherit movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
         //individual movement 2
        mMat.translate(0.0f, 0.638079f, -0.738314f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.638079f, 0.738314f);
         }
         if (modelIndex==4) {
         //inherit movement 2
        mMat.translate(0.0f, 0.638079f, -0.738314f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.638079f, 0.738314f);
        //inherit movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
        //individaul movement 3
        mMat.translate(0.0f, 0.45f, -1.2f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.45f, 1.2f); 
        }
      
      }if (movFin2){       //finger 2 movement
         if (modelIndex==5) {
        //individual movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);   
         }
         if (modelIndex==6) {
        //inherit movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
        //individual movement 2
        mMat.translate(0.0f, 0.638079f, -0.738314f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.638079f, 0.738314f);
        }
        if (modelIndex==7) {
        //inherit movement 2
        mMat.translate(0.0f, 0.638079f, -0.738314f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.638079f, 0.738314f);
        //inherit movement 1
        mMat.translate(0.0f, 0.343812f, 0.140536f);
        mMat.rotateXYZ((float)(0.3f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.343812f, -0.140536f);
        //individaul movement 3
        mMat.translate(0.0f, 0.45f, -1.2f);
        mMat.rotateXYZ((float)(0.75f*Math.sin(2f*tf)),0.0f, 0.0f);
        mMat.translate(0.0f, -0.45f, 1.2f);           }     
      
      } if (movThum) {   //thumb movement
         if (modelIndex==8) {
        //individual momement 1
        mMat.translate(-0.613613f, 0.0f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.613613f, 0.0f, 0.0f); 
        }
         if (modelIndex==9) {
        //inherit movement 1
        mMat.translate(-0.613613f, 0.0f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.613613f, 0.0f, 0.0f); 
        //individual movement 2
        mMat.translate(-0.821455f, -0.35f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.821455f, 0.35f, 0.0f);    
        }
        if (modelIndex==10) {
        //inherit movement 2
        mMat.translate(-0.821455f, -0.399856f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.821455f, 0.399856f, 0.0f); 
        //individual movement 3
        mMat.translate(-0.803631f, -0.574026f, 0.0f); 
        mMat.rotateXYZ(0.0f, (float)(0.3f*Math.sin(2f*tf)), 0.0f);
        mMat.translate(0.803631f, 0.574026f, 0.0f); 
        }
      }
            
      if (modelIndex == 0){
        mMat.identity();
        mMat.translate(loc.x(), loc.y(), loc.z());  // floor location
        mMat.scale(3.0f, 3.0f, 3.0f);  // Scale the floor after translation
		}
		
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
		
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
		
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0+ modelIndex*3]); //vertices
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1+ modelIndex*3]); //normals
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);	
      
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2+ modelIndex*3]); //textures
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_DEPTH_TEST);
      gl.glDepthFunc(GL_LEQUAL);
      
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());
   } 

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
   
		renderingProgram1 = Utils.createShaderProgram("code/vert1shader.glsl", "code/frag1shader.glsl");
		renderingProgram2 = Utils.createShaderProgram("code/vert2shader.glsl", "code/frag2shader.glsl");
      renderingProgramCubeMap = Utils.createShaderProgram("code/vertCShader.glsl", "code/fragCShader.glsl"); //Skybox
      
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

      floor = new ImportedModel("Floor.obj");
      palm = new ImportedModel("palm.obj");
      fin1_1 = new ImportedModel("fin1_1.obj");
      fin1_2 = new ImportedModel("fin1_2.obj");
      fin1_3 = new ImportedModel("fin1_3.obj");
      fin2_1 = new ImportedModel("fin2_1.obj");
      fin2_2 = new ImportedModel("fin2_2.obj");
      fin2_3 = new ImportedModel("fin2_3.obj");
      thm1_1 = new ImportedModel("thm1_1.obj");
      thm1_2 = new ImportedModel("thm1_2.obj");
      thm1_3 = new ImportedModel("thm1_3.obj");
      
      setupVertices(floor, 0);
      setupVertices(palm, 1);
      setupVertices(fin1_1, 2);
      setupVertices(fin1_2, 3);
      setupVertices(fin1_3, 4);
      setupVertices(fin2_1, 5);
      setupVertices(fin2_2, 6);
      setupVertices(fin2_3, 7);
      setupVertices(thm1_1, 8);
      setupVertices(thm1_2, 9);
      setupVertices(thm1_3, 10);
      
		setupShadowBuffers();
      
		startTime = System.currentTimeMillis();
         
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
 		skyboxTexture = Utils.loadCubeMap("cubeMap");
      gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
      		
		b.set(
			0.5f, 0.0f, 0.0f, 0.5f,
			0.0f, 0.5f, 0.0f, 0.5f,
			0.0f, 0.0f, 0.5f, 0.5f,
			0.0f, 0.0f, 0.0f, 1.0f);
	}
   
	private void setupShadowBuffers()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();
	
		gl.glGenFramebuffers(1, shadowBuffer, 0);
	
		gl.glGenTextures(1, shadowTex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
      
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
						scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
                  
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		
		// may reduce shadow border artifacts clamp to edge
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

   private void setupVertices(ImportedModel myModel, int modelIndex)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
      numObjVertices = myModel.getNumVertices();
		Vector3f[] vertices = myModel.getVertices();
		Vector2f[] texCoords = myModel.getTexCoords();
		Vector3f[] normals = myModel.getNormals();
		
		float[] pvalues = new float[numObjVertices*3];
		float[] tvalues = new float[numObjVertices*2];
		float[] nvalues = new float[numObjVertices*3];
		
		for (int i=0; i<numObjVertices; i++)
		{	pvalues[i*3]   = (float) (vertices[i]).x();
			pvalues[i*3+1] = (float) (vertices[i]).y();
			pvalues[i*3+2] = (float) (vertices[i]).z();
			tvalues[i*2]   = (float) (texCoords[i]).x();
			tvalues[i*2+1] = (float) (texCoords[i]).y();
			nvalues[i*3]   = (float) (normals[i]).x();
			nvalues[i*3+1] = (float) (normals[i]).y();
			nvalues[i*3+2] = (float) (normals[i]).z();
		}

		// for the buffers for the objects
      if (modelIndex==0){
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);
		}

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0 + modelIndex*3]); //v
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1 + modelIndex*3]); //n
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL_STATIC_DRAW);
      
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2 + modelIndex*3]); //t
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);
		 
      //to draw the cube for skybox
      float[] cubeVertexPositions =
		{	-1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f,  1.0f, -1.0f, -1.0f,  1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f, -1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f,  1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f, -1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,
			-1.0f,  1.0f, -1.0f, 1.0f,  1.0f, -1.0f, 1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f, -1.0f
		};
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[39]);
		FloatBuffer cvertBuf = Buffers.newDirectFloatBuffer(cubeVertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cvertBuf.limit()*4, cvertBuf, GL_STATIC_DRAW);
   }
	
	private void installLights(int renderingProgram)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		lightPos[0]=currentLightPos.x(); 
      lightPos[1]=currentLightPos.y(); 
      lightPos[2]=currentLightPos.z();
		
		// set current material values
		matAmb = thisAmb;
		matDif = thisDif;
		matSpe = thisSpe;
		matShi = thisShi;
		
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
	
		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}

	public static void main(String[] args) { new Code(); }
	public void dispose(GLAutoDrawable drawable) {}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupShadowBuffers();
	}
   
   public void keyPressed(KeyEvent e) 
   {
         if (e.getKeyCode()==KeyEvent.VK_UP){
                rotX -= incR;
         }else if (e.getKeyCode()==KeyEvent.VK_DOWN){
                rotX += incR;
         }else if (e.getKeyCode()==KeyEvent.VK_LEFT){
                rotY -= incR;
         }else if (e.getKeyCode()==KeyEvent.VK_RIGHT){
                rotY += incR;    
         }else if (e.getKeyCode()==KeyEvent.VK_E){
                rotLightY += incR;
         }else if (e.getKeyCode()==KeyEvent.VK_R){
                rotateSun = !rotateSun;
         }else if (e.getKeyCode()==KeyEvent.VK_L){
                chngFill = !chngFill;
         }else if (e.getKeyCode()==KeyEvent.VK_1){
                movFin1 = !movFin1; 
         }else if (e.getKeyCode()==KeyEvent.VK_2){
                movFin2 = !movFin2; 
         }else if (e.getKeyCode()==KeyEvent.VK_3){
              movThum = !movThum;
         }else if (e.getKeyCode() == KeyEvent.VK_W) { // Move light up
            lightLoc.y += 0.1f; 
         }else if (e.getKeyCode() == KeyEvent.VK_S) { // Move light down
             lightLoc.y -= 0.1f;
         } else if (e.getKeyCode() == KeyEvent.VK_A) { // Move light left
             lightLoc.x -= 0.1f;
         } else if (e.getKeyCode() == KeyEvent.VK_D) { // Move light right
             lightLoc.x += 0.1f; 
         }else System.out.println(e.getKeyCode()+" is pressed");
    }
       
    public void keyReleased(KeyEvent e) 
    {
       System.out.println("keyReleased");
    }
    public void keyTyped(KeyEvent e) 
    {
       System.out.println("keyTyped");
    }
}