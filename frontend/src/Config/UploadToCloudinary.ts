interface CloudinaryResponse {
    secure_url?: string
    url?: string
}

export const uploadToCloudinary = async (file: File): Promise<string | null> => {
    if (!file) {
        return null
    }


    try {
        const data = new FormData()
        data.append("file", file)
        data.append("upload_preset", "instagram")
        data.append("cloud_name", "dqg5pghlu")

        const isVideo = file.type.startsWith("video")
        const endpoint = isVideo
            ? "https://api.cloudinary.com/v1_1/dqg5pghlu/video/upload"
            : "https://api.cloudinary.com/v1_1/dqg5pghlu/image/upload"


        const res = await fetch(endpoint, { method: "POST", body: data })

        if (!res.ok) {
            const errorText = await res.text()
            throw new Error(`Upload failed: ${res.status}`)
        }

        const fileData: CloudinaryResponse = await res.json()

        const url = fileData.secure_url || fileData.url

        if (!url) {
            throw new Error("No URL returned from Cloudinary")
        }


        return url

    } catch (error) {
        const err = error as Error
        alert(`Lỗi upload file ${file.name}: ${err.message}`)
        return null
    }
}

// Keep old export name for backward compatibility
export const uploadToCloudnary = uploadToCloudinary
