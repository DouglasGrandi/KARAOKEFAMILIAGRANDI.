package com.familiagrandi.karaoke

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familiagrandi.karaoke.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val allVideos = mutableListOf<DriveVideo>()
    private val filtered = mutableListOf<DriveVideo>()
    private val queue = ArrayDeque<DriveVideo>()
    private lateinit var adapter: VideoAdapter

    private var score = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) startScoring()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        player = ExoPlayer.Builder(this).build()

        adapter = VideoAdapter(filtered, onAdd = {
            queue.addLast(it)
            Toast.makeText(this, "Adicionado à fila: ${'$'}{it.name}", Toast.LENGTH_SHORT).show()
        }, onPlayNow = {
            play(it)
        })

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.refreshButton.setOnClickListener { fetchFromDrive() }
        binding.playNext.setOnClickListener {
            val next = queue.removeFirstOrNull()
            if (next != null) play(next) else Toast.makeText(this, "Fila vazia", Toast.LENGTH_SHORT).show()
        }
        binding.search.addTextChangedListener {
            filter(it?.toString().orEmpty())
        }

        fetchFromDrive()
        ensureMicPermission()
    }

    private fun ensureMicPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startScoring()
        }
    }

    private fun startScoring() {
        // Extremely simple mic-based score: increments when stable pitch is detected
        val sampleRate = 22050
        val bufferSize = 1024
        val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, 0)
        val pitchProcessor = PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN, sampleRate.toFloat(), bufferSize) { res ->
            if (res.pitch > 50 && res.pitch < 1200 && res.probability > 0.8) {
                score += 1
                runOnUiThread { binding.scoreText.text = "Pontuação: ${'$'}score" }
            }
        }
        dispatcher.addAudioProcessor(pitchProcessor)
        Thread(dispatcher, "Mic-Scoring").start()
    }

    private fun fetchFromDrive() {
        val apiKey = BuildConfig.GOOGLE_DRIVE_API_KEY
        val folderId = getString(R.string.folder_id)

        scope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://www.googleapis.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val service = retrofit.create(DriveApi::class.java)
                val query = "'${'$'}folderId' in parents and mimeType contains 'video/' and trashed=false"
                val resp = withContext(Dispatchers.IO) { service.listFiles(query, key = apiKey) }
                allVideos.clear()
                allVideos.addAll(resp.files)
                filter(binding.search.text?.toString().orEmpty())
                Toast.makeText(this@MainActivity, "Encontrados ${'$'}{allVideos.size} vídeos", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Erro ao buscar Drive: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun filter(text: String) {
        filtered.clear()
        if (text.isBlank()) {
            filtered.addAll(allVideos)
        } else {
            val t = text.lowercase()
            filtered.addAll(allVideos.filter { it.name.lowercase().contains(t) })
        }
        adapter.notifyDataSetChanged()
    }

    private fun play(video: DriveVideo) {
        val url = video.streamUrl(BuildConfig.GOOGLE_DRIVE_API_KEY)
        player.stop()
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
        Toast.makeText(this, "Tocando: ${'$'}{video.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        player.release()
    }
}

class VideoAdapter(
    private val items: List<DriveVideo>,
    private val onAdd: (DriveVideo) -> Unit,
    private val onPlayNow: (DriveVideo) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VH>() {

    inner class VH(val root: android.view.View) : RecyclerView.ViewHolder(root) {
        val title: TextView = root.findViewById(R.id.title)
        val subtitle: TextView = root.findViewById(R.id.subtitle)
        val addBtn: Button = root.findViewById(R.id.addQueue)
        val playBtn: Button = root.findViewById(R.id.playNow)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.subtitle.text = item.mimeType
        holder.addBtn.setOnClickListener { onAdd(item) }
        holder.playBtn.setOnClickListener { onPlayNow(item) }
    }
}