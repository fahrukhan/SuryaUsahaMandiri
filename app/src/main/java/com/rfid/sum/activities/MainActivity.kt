package com.rfid.sum.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.port.Adapt
import com.rfid.sum.R
import com.rfid.sum.SumApplication
import com.rfid.sum.activities.ui.login.LoginActivity
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.data.constant.MenuGrid
import com.rfid.sum.data.room.DaoViewModel
import com.rfid.sum.data.room.DaoViewModelFactory
import com.rfid.sum.databinding.ActivityMainBinding
import com.rfid.sum.utils.Ui
import com.rfid.sum.widget.BGABadgeMaterialButton
import java.io.Reader

class MainActivity : BaseBinding<ActivityMainBinding>() {
    override fun getViewBinding() = ActivityMainBinding.inflate(layoutInflater)
    override fun useReader() = false

    val mainMenu = MenuGrid.menu
    private val adapter = MenuGridAdapter()
    private val viewModel: DaoViewModel by viewModels{
        DaoViewModelFactory((application as SumApplication).repository)
    }
    private var doubleClickExit = false

    override fun onStart() {
        super.onStart()
        if (!session.isLogin()){
            Ui.intent<LoginActivity>(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initMain();
    }

    private fun initMain() {
        bind.mainGrid.adapter = adapter
        bind.logoutBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure to logout?")
                .setPositiveButton("Confirm"){ d, _ ->
                    session.clear()
                    d.dismiss()
                    finish()
                }.setNegativeButton("Cancel"){ d, _ ->
                    d.dismiss()
                }.show()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {

                if (doubleClickExit){
                    moveTaskToBack(true)
                    return
                }

                doubleClickExit = true
                tos.info("Please click BACK again to exit")

                Handler(Looper.myLooper()!!).postDelayed(
                    { doubleClickExit  = false }, 2000)
            }
        })
        bind.userNameTv.text = session.getUser()?.name
    }

    inner class MenuGridAdapter : BaseAdapter() {

        override fun getCount(): Int {
            return mainMenu.size
        }

        override fun getItem(p0: Int): Any {
            return mainMenu[p0]
        }

        override fun getItemId(p0: Int): Long {
            return 0
        }

        @SuppressLint("InflateParams", "ViewHolder")
        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            val view = LayoutInflater.from(bind.root.context).inflate(R.layout.bean_grid_menu_new, bind.root, false)
            val menuTitle: TextView = view.findViewById(R.id.title)
            val menuIcon: ImageView = view.findViewById(R.id.icon)
            val cardView: CardView = view.findViewById(R.id.card)



            menuTitle.text = mainMenu[p0].title
            menuIcon.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, mainMenu[p0].icon))
            val color = ContextCompat.getColor(this@MainActivity, mainMenu[p0].color)

            val roles = session.getUser()?.user_roles?.any { it.role!! == mainMenu[p0].roleName } ?: false

            cardView.setCardBackgroundColor(if (mainMenu[p0].reqAuthorization && !roles){
                getColor(R.color.lightBackground)
            }else{ Color.WHITE })


            cardView.setOnClickListener {
                if (mainMenu[p0].reqAuthorization && !roles){
                    tos.error("Forbidden!")
                    return@setOnClickListener
                }

                if (mainMenu[p0].clazz != null){
                    val qty = mainMenu[p0].qty
                    val intent = Intent(this@MainActivity, mainMenu[p0].clazz)

                    if (mainMenu[p0].needReader){
                        wait.show("Load device")

                        Handler(Looper.myLooper()!!).postDelayed({
                            startActivity(intent)
                        }, 200)
                        Handler(Looper.myLooper()!!).postDelayed({
                            wait.hide()
                        }, 2000)
                    }else{
                        Handler(Looper.myLooper()!!).postDelayed({
                            startActivity(intent)
                        }, 200)
                    }
                }
            }
            return view
        }
    }

    override fun onResume() {
        super.onResume()
        wait.hide()
    }
}