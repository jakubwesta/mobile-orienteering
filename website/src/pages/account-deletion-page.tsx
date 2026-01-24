import { Info } from "lucide-react"

const AccountDeletionPage = () => {
  return (
    <div className="min-h-screen w-full bg-background">
      <section className="py-16 px-4">
        <div className="container mx-auto max-w-4xl">
          <div className="bg-background/60 backdrop-blur-md rounded-2xl p-8 md:p-12 border border-border/50 shadow-2xl">
            <h1 className="text-4xl md:text-5xl font-bold tracking-tight mb-8">
              Account Deletion
            </h1>
            
            {/* Information Card */}
            <div className="bg-blue-500/10 border border-blue-500/20 rounded-lg p-6 flex gap-4">
              <Info className="w-6 h-6 shrink-0 text-blue-500 mt-1" />
              <div>
                <h2 className="text-xl font-semibold mb-2">
                  Account Creation Not Yet Available
                </h2>
                <p className="text-muted-foreground leading-relaxed">
                  Mobile Orienteering does not currently support user account creation. 
                  This page will be activated once the account system is implemented in a future update.
                </p>
                <p className="text-muted-foreground leading-relaxed mt-4">
                  All data is currently stored locally on your device. If you wish to remove 
                  the app and its data, simply uninstall the application from your Android device.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}

export default AccountDeletionPage
