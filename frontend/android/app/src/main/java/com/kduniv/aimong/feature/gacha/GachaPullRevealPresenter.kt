package com.kduniv.aimong.feature.gacha

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.OverlayGachaPullRevealBinding

object GachaPullRevealPresenter {

    fun show(fragment: Fragment, reveal: GachaPullRevealUi, onDismiss: () -> Unit) {
        val context = fragment.requireContext()
        val binding = OverlayGachaPullRevealBinding.inflate(LayoutInflater.from(context))

        val dialog = Dialog(context, R.style.TransparentDialog).apply {
            setContentView(binding.root)
            setCancelable(false)
            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = Color.parseColor("#E6101828")
                navigationBarColor = Color.parseColor("#E6101828")
            }
        }

        binding.tvRevealTitle.text = when {
            reveal.isNew -> context.getString(R.string.gacha_pull_reveal_title_new)
            reveal.fragmentsGot > 0 -> context.getString(R.string.gacha_pull_reveal_title_duplicate)
            else -> context.getString(R.string.gacha_pull)
        }

        PetArtAssets.bindSprite(
            image = binding.ivRevealSprite,
            emojiFallback = binding.tvRevealEmoji,
            petType = reveal.petType,
            stage = "EGG",
            emoji = reveal.emoji,
        )
        binding.tvRevealName.text = reveal.displayName
        binding.tvRevealGrade.text = context.getString(
            R.string.gacha_pet_grade_fmt,
            GachaUiMapper.gradeLabel(reveal.grade)
        )

        binding.tvRevealBadge.text = when {
            reveal.isNew -> context.getString(R.string.gacha_pull_reveal_badge_new)
            reveal.fragmentsGot > 0 ->
                context.getString(R.string.gacha_pull_reveal_badge_fragments_fmt, reveal.fragmentsGot)
            reveal.levelUp -> context.getString(R.string.gacha_pull_reveal_badge_level_up)
            else -> GachaUiMapper.gradeLabel(reveal.grade)
        }

        val gradeLine = GachaUiMapper.gradeLabel(reveal.grade)
        val detailMain = context.getString(
            R.string.gacha_pull_reveal_detail_fmt,
            gradeLine,
            reveal.remainingTickets
        )
        binding.tvRevealDetail.text = if (reveal.levelUp) {
            "$detailMain\n${context.getString(R.string.gacha_pull_reveal_detail_level_up)}"
        } else {
            detailMain
        }

        binding.btnRevealConfirm.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setOnDismissListener { onDismiss() }

        dialog.show()
        playRevealAnimation(binding)
    }

    private fun playRevealAnimation(binding: OverlayGachaPullRevealBinding) {
        binding.layoutRevealRoot.alpha = 0f
        binding.cardReveal.apply {
            cameraDistance = 12000f * resources.displayMetrics.density
            scaleX = 0.55f
            scaleY = 0.55f
            rotationY = 88f
            alpha = 0f
        }
        binding.tvRevealTitle.translationY = 24f
        binding.tvRevealTitle.alpha = 0f

        binding.layoutRevealRoot.animate()
            .alpha(1f)
            .setDuration(220)
            .start()

        binding.tvRevealTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.cardReveal.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .rotationY(0f)
            .setDuration(620)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.flRevealPet.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .setDuration(140)
                    .withEndAction {
                        binding.flRevealPet.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start()
                    }
                    .start()
            }
            .start()

        binding.tvRevealDetail.alpha = 0f
        binding.btnRevealConfirm.alpha = 0f
        binding.tvRevealDetail.animate().alpha(1f).setStartDelay(480).setDuration(280).start()
        binding.btnRevealConfirm.animate().alpha(1f).setStartDelay(560).setDuration(280).start()
    }
}
