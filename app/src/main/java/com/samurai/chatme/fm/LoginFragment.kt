package com.samurai.chatme.fm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.samurai.chatme.R
import com.samurai.chatme.databinding.FragmentLoginBinding
import com.samurai.chatme.util.TypingAnimationUtil

@Suppress("DEPRECATION")
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var typingAnimation: TypingAnimationUtil
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        typingAnimation = TypingAnimationUtil(
            binding.textView, // bu siz qo'ygan TextView
            "Bizga qo'shiling..."
        )
        typingAnimation.start()


        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()


        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)



        binding.btnGoogleSignIn.setOnClickListener {
            typingAnimation.stop()
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, RC_SIGN_IN)
        }
    }

    @Deprecated("Deprecated API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { taskResult ->
                    if (taskResult.isSuccessful) {
                        saveUserToDatabase()
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    } else {

                    }
                }
            }
        }
    }

    private fun saveUserToDatabase() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val name = user.displayName ?: "Unknown"
        val photo = user.photoUrl?.toString() ?: ""

        val map = mapOf(
            "uid" to uid,
            "name" to name,
            "photo" to photo
        )

        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .setValue(map)
    }
}
