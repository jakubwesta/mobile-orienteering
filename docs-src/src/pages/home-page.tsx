import { Map, MapPin, Timer, BarChart3, BookOpen, Cloud } from "lucide-react"
import { Link } from "react-router-dom"

import { PlayStoreButton } from "@/components/features/play-store-button"
import { GitHubButton } from "@/components/features/github-button"
import bannerBackground from "@/assets/banner_background.png"

const HomePage = () => {
  const features = [
    {
      title: "Interactive Map View",
      description: "View orienteering maps with real-time GPS positioning",
      icon: Map,
      color: "blue"
    },
    {
      title: "Checkpoint Navigation",
      description: "Mark and navigate to checkpoints during your run",
      icon: MapPin,
      color: "green"
    },
    {
      title: "Run Tracking",
      description: "Record your activities with detailed time and distance metrics",
      icon: Timer,
      color: "orange"
    },
    {
      title: "Performance Analytics",
      description: "Analyze your runs with comprehensive statistics and timelines",
      icon: BarChart3,
      color: "purple"
    },
    {
      title: "Map Library",
      description: "Create custom orienteering maps",
      icon: BookOpen,
      color: "pink"
    },
    {
      title: "Cloud Sync",
      description: "Sync your maps and activities across devices",
      icon: Cloud,
      color: "cyan"
    }
  ]

  const colorClasses = {
    blue: "bg-blue-500/10 border-blue-500/20 hover:bg-blue-500/15 dark:bg-blue-500/20 dark:border-blue-500/30",
    green: "bg-green-500/10 border-green-500/20 hover:bg-green-500/15 dark:bg-green-500/20 dark:border-green-500/30",
    orange: "bg-orange-500/10 border-orange-500/20 hover:bg-orange-500/15 dark:bg-orange-500/20 dark:border-orange-500/30",
    purple: "bg-purple-500/10 border-purple-500/20 hover:bg-purple-500/15 dark:bg-purple-500/20 dark:border-purple-500/30",
    pink: "bg-pink-500/10 border-pink-500/20 hover:bg-pink-500/15 dark:bg-pink-500/20 dark:border-pink-500/30",
    cyan: "bg-cyan-500/10 border-cyan-500/20 hover:bg-cyan-500/15 dark:bg-cyan-500/20 dark:border-cyan-500/30"
  }

  return (
    <div className="min-h-screen w-full bg-background">

      {/* Hero Section */}
      <section 
        className="pt-16 pb-16 px-4 relative overflow-hidden"
      >
        {/* Semi-transparent overlay for readability */}
        <div className="absolute inset-0 bg-background/80 backdrop-blur-sm"></div>
        <img src={bannerBackground} alt="Banner Background" className="absolute inset-0 w-full h-full object-cover" />
        <div className="container mx-auto max-w-6xl text-center relative z-10">
          <div className="bg-background/60 backdrop-blur-md rounded-2xl p-8 md:p-12 border border-border/50 shadow-2xl">
            <h1 className="text-5xl md:text-6xl font-bold tracking-tight mb-6" >
            Mobile Orienteering
              <span className="block text-primary mt-2">Track, Navigate, Analyze</span>
            </h1>
            <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto" >
              A comprehensive Android application designed for orienteering training and competition. 
              Track your runs, navigate checkpoints, and analyze your performance with detailed statistics.
            </p>
            
            {/* Download Buttons */}
            <div className="flex flex-col sm:flex-row gap-6 justify-center items-center">
            <PlayStoreButton className="h-16! w-56 text-lg" />

            <GitHubButton className="h-16 w-56 text-lg" />
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-16 px-4 bg-muted/30">
        <div className="container mx-auto max-w-6xl">
          <h2 className="text-3xl md:text-4xl font-bold text-center mb-12">
            Key Features
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feature, index) => {
              const IconComponent = feature.icon
              return (
                <div 
                  key={index}
                  className={`p-6 rounded-lg border min-h-40 md:min-h-0 ${colorClasses[feature.color as keyof typeof colorClasses]} hover:shadow-lg transition-all`}
                >
                  <div className="flex items-start justify-between mb-2 gap-4">
                    <h3 className="text-xl font-semibold flex-1">{feature.title}</h3>
                    <IconComponent className="w-8 h-8 shrink-0" />
                  </div>
                  <p className="text-muted-foreground">{feature.description}</p>
                </div>
              )
            })}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t py-4 px-4">
        <div className="container mx-auto max-w-6xl text-center text-sm text-muted-foreground">
          <p className="mb-2">
            Created by Jakub Westa & Dawid Pilarski
          </p>
          <p>
            Licensed under Elastic License 2.0 • {" "}
            <a 
              href="https://github.com/jakubwesta/mobile-orienteering" 
              target="_blank" 
              rel="noopener noreferrer"
              className="hover:text-foreground transition-colors underline"
            >
              View on GitHub
            </a>
            {" "} • {" "}
            <Link 
              to="/privacy-policy" 
              className="hover:text-foreground transition-colors underline"
            >
              Privacy Policy
            </Link>
          </p>
        </div>
      </footer>
    </div>
  )
}

export default HomePage
