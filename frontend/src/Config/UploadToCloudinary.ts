interface CloudinaryResponse {
    secure_url?: string
    url?: string
}

export const uploadToCloudinary = async (file: File): Promise<string | null> => {
    if (!file) {
        console.error("No file provided to upload")
        return null
    }

    console.log(`📤 Uploading file: ${file.name}, type: ${file.type}, size: ${file.size}`)

    try {
        const data = new FormData()
        data.append("file", file)
        data.append("upload_preset", "instagram")
        data.append("cloud_name", "dqg5pghlu")

        const isVideo = file.type.startsWith("video")
        const endpoint = isVideo
            ? "https://api.cloudinary.com/v1_1/dqg5pghlu/video/upload"
            : "https://api.cloudinary.com/v1_1/dqg5pghlu/image/upload"

        console.log(`📍 Uploading to endpoint: ${endpoint}`)

        const res = await fetch(endpoint, { method: "POST", body: data })

        if (!res.ok) {
            const errorText = await res.text()
            console.error("❌ Cloudinary upload failed:", res.status, errorText)
            throw new Error(`Upload failed: ${res.status}`)
        }

        const fileData: CloudinaryResponse = await res.json()
        console.log("✅ Cloudinary response:", fileData)

        const url = fileData.secure_url || fileData.url

        if (!url) {
            console.error("❌ No URL returned from Cloudinary:", fileData)
            throw new Error("No URL returned from Cloudinary")
        }

        console.log("✅ Upload successful, URL:", url)

        return url

    } catch (error) {
        const err = error as Error
        console.error("❌ Error uploading to Cloudinary:", error)
        alert(`Lỗi upload file ${file.name}: ${err.message}`)
        return null
    }
}

// Keep old export name for backward compatibility
export const uploadToCloudnary = uploadToCloudinary
