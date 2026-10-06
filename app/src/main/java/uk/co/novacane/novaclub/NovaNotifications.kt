// Same package name as NovaScreens.kt, so the screens can send notifications
package com.novacane.novaclub

// Android's permission and system-service tools
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
// Our own notification layouts
import android.widget.RemoteViews
// Drawing the banner and the large icon
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.core.content.res.ResourcesCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
// Colouring and bolding part of the plain-text title
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
// The "compat" versions work the same on every Android version the app supports
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
// Turns a Compose colour into the plain number Android's notifications want
import androidx.compose.ui.graphics.toArgb
// The screen to open when a notification is tapped, and the list of files in res/ (the small icon)
import uk.co.novacane.novaclub.MainActivity
import uk.co.novacane.novaclub.R

// ---------- CHANNELS ----------
// Since Android 8, every notification belongs to a channel. People can switch each channel on or off in Settings.
const val FLASH_SALE_CHANNEL = "flash_sales"

// Make the channels. Safe to call every time the app opens (Android ignores ones that already exist).
fun createNotificationChannels(context: Context) {
    // Channels only exist on Android 8 (API 26) and up
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        FLASH_SALE_CHANNEL,                       // the id we post to
        "Flash sales",                            // the name people see in Settings
        NotificationManager.IMPORTANCE_HIGH       // high = pops up over the screen with a sound
    ).apply {
        description = "Last-minute studio sessions at a discount"   // the line under the name in Settings
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

// ---------- PERMISSION ----------
// Whether we're allowed to show notifications right now
fun canNotify(context: Context): Boolean {
    // Android 13 (API 33) and up: the person has to say yes to the permission first
    val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    // On any version they can also switch the app's notifications off in Settings
    return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

// Opens this app's notification page in Settings (for when they've said no and need to change their mind)
fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Straight to the notification switches
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        // Older phones: the app's general info page
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// ---------- SENDING ----------
// Show a flash sale alert for one slot. Does nothing if notifications aren't allowed.
fun showFlashSaleAlert(context: Context, slot: FlashSlot) {
    // Check first: posting without permission is ignored on Android 13+
    if (!canNotify(context)) return

    // What happens when it's tapped: open the app (the same screen the launcher icon opens)
    val openApp = PendingIntent.getActivity(
        context,
        0,                                                                     // a request code (only matters if you have several)
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),  // reuse the app if it's already open
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT      // required on Android 12+
    )

    // The Novacane look. A normal notification can't change fonts or background, so we hand Android our own layouts.
    // Android draws notifications itself and can't load our font files, so any Novacane text is drawn into pictures.
    // Folded up: gradient strip with the title in Jost and the details in the typewriter font
    val stripTitle = "Flash sale · £${slot.price}/hr"
    val stripText = "${slot.day}, ${slot.time} · ${slot.type.label}".uppercase()
    val collapsed = RemoteViews(context.packageName, R.layout.notification_flash_collapsed).apply {
        setImageViewBitmap(R.id.notif_strip, titleStrip(context, stripTitle, stripText))
        setContentDescription(R.id.notif_strip, "$stripTitle. $stripText")       // read out by screen readers
    }
    // Pulled open: the big cosmic banner
    val expanded = RemoteViews(context.packageName, R.layout.notification_flash_expanded).apply {
        setImageViewBitmap(R.id.notif_banner, flashSaleBanner(context, slot))
        setContentDescription(R.id.notif_banner, "$stripTitle. ${slot.day}, ${slot.time}. ${slot.type.label}, normally £${slot.type.baseRate} an hour.")
    }
    // The button label, in bold Novacane magenta (newer Android ignores the accent colour on buttons otherwise)
    val bookLabel = SpannableString("Book it").apply {
        setSpan(ForegroundColorSpan(NovaMagenta.toArgb()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // Plain-text versions of the title and text, for places that don't show custom layouts (lock screen, watches,
    // screen readers). The price is in bold Novacane magenta.
    val price = "£${slot.price}/hr"
    val title = SpannableString("Flash sale · $price").apply {
        val start = length - price.length                                                             // where the price starts
        setSpan(ForegroundColorSpan(NovaMagenta.toArgb()), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)  // magenta
        setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)            // bold
    }
    val text = "${slot.day}, ${slot.time}. ${slot.type.label}, normally £${slot.type.baseRate}/hr."

    // Build the notification
    val notification = NotificationCompat.Builder(context, FLASH_SALE_CHANNEL)
        .setSmallIcon(R.drawable.ic_stat_nova)                                  // the little white star in the status bar
        .setColor(NovaMagenta.toArgb())                                         // accent: tints the small icon and app name
        .setContentTitle(title)                                                 // plain-text title (see above)
        .setContentText(text)                                                   // plain-text details
        .setStyle(NotificationCompat.DecoratedCustomViewStyle())                // keep Android's header (icon, app name, time) round our layouts
        .setCustomContentView(collapsed)                                        // folded-up layout
        .setCustomBigContentView(expanded)                                      // pulled-open layout
        .setCustomHeadsUpContentView(collapsed)                                 // the pop-up at the top of the screen
        .addAction(R.drawable.ic_stat_nova, bookLabel, openApp)                 // a button under the notification
        .setPriority(NotificationCompat.PRIORITY_HIGH)                          // pop-up on Android 7 (channels handle this on 8+)
        .setCategory(NotificationCompat.CATEGORY_PROMO)                         // tells Android it's an offer
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)                    // nothing private, so show it all on the lock screen
        .setContentIntent(openApp)                                              // what tapping does
        .setAutoCancel(true)                                                    // disappears once tapped
        .build()

    // Post it. Using the slot's details as the id means the same slot replaces its old alert instead of stacking.
    NotificationManagerCompat.from(context).notify("${slot.day} ${slot.time}".hashCode(), notification)
}

// ---------- PICTURES ----------
// Notifications can't use Compose, so these are drawn with Android's older Canvas, in the same style as the app.

// The folded-up notification's two lines: the title in Jost, the details in the typewriter font.
// Drawn at the phone's real pixel size so it's as sharp as normal text.
private fun titleStrip(context: Context, title: String, details: String): Bitmap {
    val dp = context.resources.displayMetrics.density                          // pixels per dp on this phone
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.jost_semibold)
        textSize = 16f * dp
        color = NovaWhite.toArgb()
    }
    val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.source_code_pro_regular)
        textSize = 11f * dp
        letterSpacing = 0.08f
        color = NovaBlush.toArgb()
    }
    // Just wide enough for the longer line, and 40dp tall (the same as its spot in the layout)
    val width = maxOf(titlePaint.measureText(title), detailPaint.measureText(details)) + 2f * dp
    val bitmap = Bitmap.createBitmap(width.toInt(), (40f * dp).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawText(title, 0f, 18f * dp, titlePaint)                           // first line
    canvas.drawText(details, 0f, 34f * dp, detailPaint)                        // second line
    return bitmap
}

// The big banner: deep space, nebulae, stars, a planet's horizon lit by a rising star, the ringed sigil, and the deal
private fun flashSaleBanner(context: Context, slot: FlashSlot): Bitmap {
    val w = 1024f                                                              // 2:1, the shape Android expects for big pictures
    val h = 512f
    val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Rounded corners, so it sits neatly inside the gradient card
    canvas.clipPath(android.graphics.Path().apply { addRoundRect(RectF(0f, 0f, w, h), 28f, 28f, android.graphics.Path.Direction.CW) })

    // A soft round glow: bright in the middle, clear at the edge
    fun glow(color: Color, x: Float, y: Float, radius: Float) {
        paint.shader = RadialGradient(x, y, radius, color.toArgb(), Color.Transparent.toArgb(), Shader.TileMode.CLAMP)
        canvas.drawCircle(x, y, radius, paint)
        paint.shader = null
    }

    // --- Deep space and nebulae ---
    canvas.drawColor(NovaVoid.toArgb())
    glow(NovaMagenta.copy(alpha = 0.55f), w * 0.12f, h * 0.05f, w * 0.62f)     // magenta, top-left
    glow(NovaPurple.copy(alpha = 0.60f), w * 0.88f, h * 0.40f, w * 0.55f)      // purple, right
    glow(NovaCobalt.copy(alpha = 0.35f), w * 0.40f, h * 0.95f, w * 0.40f)      // cobalt, low middle

    // --- Stars (the same pattern every time) ---
    val rnd = Random(42)
    repeat(110) {
        paint.color = NovaStarlight.copy(alpha = 0.2f + 0.7f * rnd.nextFloat()).toArgb()
        canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.85f, 0.8f + 2.4f * rnd.nextFloat() * rnd.nextFloat(), paint)
    }

    // --- The planet's horizon along the bottom ---
    val planetR = w * 1.6f                                                     // much wider than the picture, so it reads as a horizon
    val cx = w * 0.5f
    val cy = h * 0.88f + planetR                                               // its top just shows at the bottom
    // Magenta atmosphere hugging the edge
    paint.shader = RadialGradient(
        cx, cy, planetR * 1.08f,
        intArrayOf(Color.Transparent.toArgb(), Color.Transparent.toArgb(), NovaMagenta.copy(alpha = 0.5f).toArgb(), Color.Transparent.toArgb()),
        floatArrayOf(0f, 0.9f, 1f / 1.08f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(cx, cy, planetR * 1.08f, paint)
    // The dark side of the planet
    paint.shader = LinearGradient(0f, h * 0.88f, 0f, h, NovaPlum.toArgb(), NovaVoid.toArgb(), Shader.TileMode.CLAMP)
    canvas.drawCircle(cx, cy, planetR, paint)
    paint.shader = null
    // The rising star, two-thirds of the way across
    val sunX = w * 0.66f
    val sunY = cy - sqrt(planetR * planetR - (sunX - cx) * (sunX - cx))
    glow(NovaCorona.copy(alpha = 0.55f), sunX, sunY, w * 0.30f)
    // The rim light: gold where the star is, fading to faint pink
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    paint.shader = LinearGradient(
        0f, 0f, w, 0f,
        intArrayOf(NovaPink.copy(alpha = 0.1f).toArgb(), NovaCorona.toArgb(), NovaPink.copy(alpha = 0.1f).toArgb()),
        floatArrayOf(0f, 0.66f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(cx, cy, planetR, paint)
    paint.shader = null
    paint.style = Paint.Style.FILL
    paint.color = NovaStarlight.toArgb()
    canvas.drawCircle(sunX, sunY, 6f, paint)                                   // the star itself

    // --- The sigil as a ringed planet, on the right ---
    val sx = w * 0.80f                                                         // centre across
    val sy = h * 0.42f                                                         // centre down
    val sigilSize = h * 0.46f
    val ring = RectF(sx - sigilSize * 0.95f, sy - sigilSize * 0.22f, sx + sigilSize * 0.95f, sy + sigilSize * 0.22f)
    glow(NovaCorona.copy(alpha = 0.22f), sx, sy, sigilSize * 0.9f)            // a soft corona behind it
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    canvas.save()
    canvas.rotate(-14f, sx, sy)                                                // tip the ring over
    paint.color = NovaBlush.copy(alpha = 0.35f).toArgb()
    canvas.drawArc(ring, 180f, 180f, false, paint)                             // back half, behind the sigil
    canvas.restore()
    ContextCompat.getDrawable(context, R.drawable.novacane_logo)?.apply {      // the sigil
        setBounds((sx - sigilSize / 2).toInt(), (sy - sigilSize / 2).toInt(), (sx + sigilSize / 2).toInt(), (sy + sigilSize / 2).toInt())
        draw(canvas)
    }
    canvas.save()
    canvas.rotate(-14f, sx, sy)
    paint.color = NovaBlush.copy(alpha = 0.75f).toArgb()
    canvas.drawArc(ring, 0f, 180f, false, paint)                               // front half, in front of the sigil
    paint.style = Paint.Style.FILL
    val moonAngle = 0.7f                                                       // where the moon sits on the ring (radians)
    val mx = sx + ring.width() / 2f * cos(moonAngle)
    val my = sy + ring.height() / 2f * sin(moonAngle)
    canvas.restore()
    glow(NovaPink.copy(alpha = 0.6f), mx, my, 22f)                             // the moon's halo
    paint.color = NovaPink.toArgb()
    canvas.drawCircle(mx, my, 7f, paint)                                       // the moon

    // --- The writing, on the left ---
    val jost = ResourcesCompat.getFont(context, R.font.jost_bold)
    val jostMedium = ResourcesCompat.getFont(context, R.font.jost_medium)
    val mono = ResourcesCompat.getFont(context, R.font.source_code_pro_semibold)
    val left = 56f
    // "FLASH SALE", small, wide-spaced, in the typewriter font
    paint.typeface = mono
    paint.textSize = 28f
    paint.letterSpacing = 0.3f
    paint.color = NovaPink.toArgb()
    canvas.drawText("FLASH SALE", left, 96f, paint)
    // The price, huge
    paint.typeface = jost
    paint.textSize = 150f
    paint.letterSpacing = 0f
    paint.color = NovaWhite.toArgb()
    val priceText = "£${slot.price}"
    canvas.drawText(priceText, left, 240f, paint)
    // "/hr" just after it, smaller and pink
    val priceWidth = paint.measureText(priceText)
    paint.typeface = jostMedium
    paint.textSize = 52f
    paint.color = NovaBlush.toArgb()
    canvas.drawText("/hr", left + priceWidth + 8f, 240f, paint)
    // When it is
    paint.textSize = 40f
    paint.color = NovaWhite.toArgb()
    canvas.drawText("${slot.day}, ${slot.time}", left, 310f, paint)
    // What it is, and the normal price crossed out
    paint.typeface = mono
    paint.textSize = 24f
    paint.letterSpacing = 0.15f
    paint.color = NovaMuted.toArgb()
    val label = "${slot.type.label.uppercase()} · WAS "
    canvas.drawText(label, left, 360f, paint)
    paint.isStrikeThruText = true
    canvas.drawText("£${slot.type.baseRate}", left + paint.measureText(label), 360f, paint)
    paint.isStrikeThruText = false
    // How soon, and the hook
    paint.color = NovaPink.toArgb()
    canvas.drawText("STARTS IN ${slot.hoursAway} HOURS · FIRST TO BOOK GETS IT", left, 404f, paint)

    return bitmap
}
