import { useEffect, useState } from "react"

const PrivacyPolicyPage = () => {
  const [privacyPolicyText, setPrivacyPolicyText] = useState<string>("")

  useEffect(() => {
    fetch("/privacy-policy.txt")
      .then((response) => response.text())
      .then((text) => setPrivacyPolicyText(text))
      .catch((error) => console.error("Error loading privacy policy:", error))
  }, [])

  return (
    <div className="min-h-screen w-full bg-background">
      <section className="py-16 px-4">
        <div className="container mx-auto max-w-4xl">
          <div className="bg-background/60 backdrop-blur-md rounded-2xl p-8 md:p-12 border border-border/50 shadow-2xl">
            <pre className="whitespace-pre-wrap font-sans text-foreground leading-relaxed">
              {privacyPolicyText}
            </pre>
          </div>
        </div>
      </section>
    </div>
  )
}

export default PrivacyPolicyPage
