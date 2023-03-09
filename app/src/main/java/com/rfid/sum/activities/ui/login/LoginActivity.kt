package com.rfid.sum.activities.ui.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.activities.MainActivity
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.UserModel
import com.rfid.sum.databinding.ActivityLoginBinding
import com.rfid.sum.rest.ApiRequest
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.SumApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.rest.model.ResponseModel
import com.rfid.sum.rest.model.TokenModel
import com.rfid.sum.utils.Ui
import org.json.JSONObject
import java.util.*

class LoginActivity : BaseBinding<ActivityLoginBinding>() {
    override fun getViewBinding() = ActivityLoginBinding.inflate(layoutInflater)
    override fun useReader() = false

    override fun onStart() {
        super.onStart()
        if (session.isLogin()) {
            Ui.intent<MainActivity>(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionCheck()
    }

    private fun permissionCheck() {
        //Check if the permission (NEED_PERMISSION) is authorized, PackageManager.PERMISSION_GRANTED means you agree with the authorization.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            //The user has already been denied once, popping up again requires an explanation
//			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
//					.WRITE_EXTERNAL_STORAGE)) {
//				ShowTip("Please enable the relevant permissions, or you will not be able to use this application!");
//			}
            //ask for permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                1
            )
        } else {
            initLogin()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1){
            permissions.forEachIndexed { i, _ ->
                if (permissions[i] == Manifest.permission.READ_PHONE_STATE){
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                        initLogin()
                    }
                }
            }

        }
    }

    private fun initLogin() {

        with(bind) {
            loginBtn.setOnClickListener {
                //Ui.intent<TagRegisterActivity>(this@LoginActivity)
                val user = loginEmail.text.toString()
                val pass = loginPassword.text.toString()

                if (user.isEmpty() || pass.isEmpty()) {
                    tos.info("Please fill out all fields!")
                    return@setOnClickListener
                }

                wait.show()
                login(user, pass)
            }
        }
    }

    private var currentToken = ""
    private fun login(user: String, pass: String) {
        //val param = paramBuilder.getParam(ApiRequest.Login)
        val param = mutableMapOf<String, Any>(
            "email" to user, "password" to pass
        )
        param.putAll(paramBuilder.getParam(ApiRequest.Login))
        val gson = Gson().toJson(param)
        volley.nciPostData(Url.LOGIN, JSONObject(gson), object: NciApiListener{
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    if (success){
                        val u = UserModel(token = data.toString())
                        if (u.token?.length!! > 40){
                            currentToken = u.token!!
                            session.saveUser(u)
                            loadUser()
                        }else{
                            wait.hide()
                            tos.info("Error while load user token.")
                        }
                    }else{
                        wait.hide()
                        tos.error(errorMsg)
                    }
                }
            }
        })


//        volley.sumPostData(Url.GET_TOKEN, JSONObject(gson), object : SumApiListener {
//            override fun onResponse(response: ResponseModel, errorMsg: String, key: Int?) {
//                with(response) {
//                    if (success) {
//                        val u = UserModel(user, pass, data.toString())
//                        session.saveUser(u)
//
//                        val cal = Calendar.getInstance()
//                        val created = cal.timeInMillis / 1000
//                        val expired = created + 3600;
//                        val mToken = TokenModel(response.requestId, response.token, created, expired)
//                        tokenMgt.saveToken(param["Method"].toString(), mToken)
//
//                        wait.hide()
//                        Ui.intent<MainActivity>(this@LoginActivity)
//                    } else {
//                        wait.hide()
//                        tos.error(errorMsg)
//                    }
//                }
//            }
//        })
    }

    private fun loadUser() {
        volley.getData(Url.USER, object: NciApiListener{
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    if (success){
                        val u = castModel<UserModel>()
                        if (u != null){
                            u.token = currentToken
                            session.saveUser(u)

                            tos.info("Berhasil login.")
                            Ui.intent<MainActivity>(this@LoginActivity)
                        }else{
                            tos.info("Error while load user data.")
                        }
                        wait.hide()
                    }else{
                        wait.hide()
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }
}
