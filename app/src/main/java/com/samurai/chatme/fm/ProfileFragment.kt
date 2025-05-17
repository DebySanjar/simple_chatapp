package com.samurai.chatme.fm

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.samurai.chatme.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.samurai.chatme.R

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserInfo()

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Rasmi uchun (Glide yoki Picasso ishlatsa bo'ladi, oddiy holatda URL ni o'qib qo'yamiz)
            val photoUrl = currentUser.photoUrl
            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.img_1) // yuklanayotgan paytdagi rasm
                    .error(R.drawable.img_1)       // rasm yuklanmasa
                    .into(binding.imgProfile)
            } else {
                binding.imgProfile.setImageResource(R.drawable.img_1)
            }


            binding.tvNickname.text = currentUser.displayName ?: "No Name"
            binding.tvEmail.text = currentUser.email ?: "No Email"
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Ogohlantiramiz")
            .setMessage("Agar hisobdan chiqsangiz barcha yozishmalaringiz xavfsizlik yuzasidan o'chrib yuboriladi, qaytganingizda ularni qayta ko'rmasligingiz mumkin!")
            .setPositiveButton("Chiqish") { dialog, _ ->
                dialog.dismiss()
                logoutUser()
            }
            .setNegativeButton("Qaytish") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun logoutUser() {
        // Yozishmalarni o'chirishni bajarish (Realtimeda o'chirish kodi)
        val uid = auth.currentUser?.uid ?: return
        val userMessagesRef = database.reference.child("messages").child(uid)
        userMessagesRef.removeValue().addOnCompleteListener {
            // Hisobdan chiqish
            auth.signOut()
            // LoginFragmentga qaytish
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
