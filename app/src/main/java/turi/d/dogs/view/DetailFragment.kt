package turi.d.dogs.view


import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.telephony.SmsManager
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.ViewModelStore
import androidx.navigation.Navigation
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.android.synthetic.main.fragment_detail.*
import turi.d.dogs.R
import turi.d.dogs.databinding.FragmentDetailBinding
import turi.d.dogs.databinding.SendSmsDialogBinding
import turi.d.dogs.model.DogBreed
import turi.d.dogs.model.DogPalette
import turi.d.dogs.model.SmsInfo
import turi.d.dogs.util.getProgressDrawable
import turi.d.dogs.util.loadImage
import turi.d.dogs.viewmodel.DetailViewModel


class DetailFragment : Fragment() {

    private lateinit var viewModel: DetailViewModel
    private var dogUuid = 0
    private lateinit var dataBinding: FragmentDetailBinding
    private var sendSmsStarted = false
    private var currentDog: DogBreed? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_detail, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            dogUuid = DetailFragmentArgs.fromBundle(it).dogUuid
        }
        viewModel = ViewModelProviders.of(this).get(DetailViewModel::class.java)
        viewModel.fetch(dogUuid)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.dogLiveData.observe(this, Observer { dog ->
            currentDog = dog
            dog?.let {
                dataBinding.dog = dog
                it.imageUrl?.let {
                    setupBackgroundColor(it)
                }
            }
        })
    }

    private fun setupBackgroundColor(url: String) {
        Glide.with(this)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Palette.from(resource)
                        .generate { palette ->
                            val intColor = palette?.vibrantSwatch?.rgb ?: 0
                            val myPalette = DogPalette(intColor)
                            dataBinding.palette = myPalette
                        }
                }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.details_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_send_sms -> {
                sendSmsStarted = true
                (activity as MainActivity).checkSmsPermission()
            }
            R.id.action_share -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_SUBJECT, "Check out this Dog Breed")
                intent.putExtra(
                    Intent.EXTRA_TEXT,
                    "${currentDog?.dogBreed} bred for ${currentDog?.bredFor} it has a lifespan of ${currentDog?.lifespan} and has these temperaments ${currentDog?.temperament}"
                )
                intent.putExtra(Intent.EXTRA_STREAM, currentDog?.imageUrl)
                startActivity(Intent.createChooser(intent, "Share with"))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onPermissionResult(permissionGranted: Boolean) {
        if (sendSmsStarted && permissionGranted) {
            context?.let {
                val smsInfo = SmsInfo(
                    "",
                    "${currentDog?.dogBreed} bred for ${currentDog?.bredFor} it has a lifespan of ${currentDog?.lifespan} and has these temperaments ${currentDog?.temperament}",
                    currentDog?.imageUrl
                )
                val dialogBinding = DataBindingUtil.inflate<SendSmsDialogBinding>(
                    LayoutInflater.from(it),
                    R.layout.send_sms_dialog,
                    null,
                    false
                )
                androidx.appcompat.app.AlertDialog.Builder(it)
                    .setView(dialogBinding.root)
                    .setPositiveButton("Send SMS") { dialog, which ->
                        if (!dialogBinding.smsDestinaton.text.isNullOrEmpty()) {
                            smsInfo.to = dialogBinding.smsDestinaton.text.toString()
                            sendSms(smsInfo)
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, which -> }
                    .show()
                dialogBinding.smsInfo = smsInfo
            }
        }
    }

    private fun sendSms(smsInfo: SmsInfo) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, intent, 0)
        val sms = SmsManager.getDefault()
        sms.sendTextMessage(smsInfo.to, null, smsInfo.text, pi, null)
    }
}
