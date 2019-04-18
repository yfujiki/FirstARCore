package com.yfujiki.firstarcore

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import com.google.ar.sceneform.ux.ArFragment

import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.content_base.*
import android.widget.Toast
import com.google.a.b.a.a.a.e
import android.content.Context.ACTIVITY_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.app.ActivityManager
import android.os.Build
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.View
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode


class BaseActivity : AppCompatActivity() {

    lateinit var fragment: ArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_base)
        setSupportActionBar(toolbar)

        fragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_base, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e("FragmentActivity", "Sceneform requires Android N or later")
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < 3.0) {
            Log.e("FragmentActivity", "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }

    fun onPlaceObject(view: View) {
        val frame = fragment.arSceneView.arFrame
        val point = getScreenCenter()
        if (frame != null) {
            val hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())
            for (hit in hits) {                 // iterate through HitResults that you tapped
                val trackable = hit.trackable   // HitResult.trackable is either Face/Image/Plane or Point
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    // Magic spell to check whether the hit point was the plane you wanted
                    placeObject(fragment, hit.createAnchor(), Uri.parse("Heart.sfb"))
                    break
                }
            }
        }
    }

//    private fun addNodeToScene(fragment: ArFragment, createAnchor: Anchor, renderable: ModelRenderable) {
//        val anchorNode = AnchorNode(createAnchor)
//
//        val transformableNode = TransformableNode(fragment.transformationSystem)
//        transformableNode.renderable = renderable
//        transformableNode.setParent(anchorNode)
//
//        fragment.arSceneView.scene.addChild(anchorNode)
//        transformableNode.select()
//    }

    private fun addNodeToScene(fragment: ArFragment, createAnchor: Anchor, renderable: ModelRenderable) {
        val anchorNode = AnchorNode(createAnchor)
        val rotatingNode = RotatingNode(fragment.transformationSystem)

//        val transformableNode = TransformableNode(fragment.transformationSystem)

        rotatingNode.renderable = renderable

//        rotatingNode.addChild(transformableNode)
        rotatingNode.setParent(anchorNode)

        fragment.arSceneView.scene.addChild(anchorNode)
        rotatingNode.select()
    }

    private fun placeObject(fragment: ArFragment, createAnchor: Anchor, model: Uri) {
        ModelRenderable.builder()
            .setSource(fragment.context, model)
            .build()
            .thenAccept {
                addNodeToScene(fragment, createAnchor, it)
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(it.message)
                    .setTitle("error!")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
    }

    private fun getScreenCenter(): android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return Point(vw.width / 2, vw.height / 2)
    }
}
